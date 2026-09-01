package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import java.math.BigDecimal
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObserveMonthlyServiceReceiptsUseCaseTest {

    private val now = Instant.parse("2026-08-16T15:00:00Z")
    private val timeProvider = object : TimeProvider {
        override fun now(): Instant = now
    }

    private val planRepo = FakeFinancialPlanRepo()
    private val receiptRepo = FakeMonthlyServiceReceiptRepo()
    private val movementRepo = FakeMovementRepo()
    private val useCase = ObserveMonthlyServiceReceiptsUseCase(planRepo, receiptRepo, movementRepo, timeProvider)

    @Test
    fun `emits configured services in pending state when no payment saved for current month`() = runBlocking {
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("3000")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("100")).getOrError(),
            electricityExpenses = Money.of(BigDecimal("20")).getOrError(),
            waterExpenses = Money.of(BigDecimal("20")).getOrError(),
            internetExpenses = Money.of(BigDecimal("20")).getOrError(),
            phoneExpenses = Money.of(BigDecimal("40")).getOrError(),
        )
        planRepo.setPlan(plan)

        val receipts = useCase().first()

        assertEquals(4, receipts.size)
        assertEquals(ServiceReceiptKey.LIGHT, receipts[0].key)
        assertEquals("2026-08", receipts[0].monthKey)
        assertFalse(receipts[0].isPaid)

        assertEquals(ServiceReceiptKey.WATER, receipts[1].key)
        assertFalse(receipts[1].isPaid)

        assertEquals(ServiceReceiptKey.INTERNET, receipts[2].key)
        assertFalse(receipts[2].isPaid)

        assertEquals(ServiceReceiptKey.PHONE, receipts[3].key)
        assertFalse(receipts[3].isPaid)
    }

    @Test
    fun `marks specific receipt as paid when payment exists for current month`() = runBlocking {
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("3000")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("100")).getOrError(),
            electricityExpenses = Money.of(BigDecimal("20")).getOrError(),
            waterExpenses = Money.of(BigDecimal("20")).getOrError(),
        )
        planRepo.setPlan(plan)

        receiptRepo.saveReceipt(
            MonthlyServiceReceipt(
                key = ServiceReceiptKey.LIGHT,
                title = "Luz",
                configuredAmount = Money.of(BigDecimal("20")).getOrError(),
                monthKey = "2026-08",
                isPaid = true,
                paidMovementId = "mov-123",
                paidAt = now,
            )
        )

        val receipts = useCase().first()

        assertEquals(2, receipts.size)
        val light = receipts.first { it.key == ServiceReceiptKey.LIGHT }
        assertTrue(light.isPaid)
        assertEquals("mov-123", light.paidMovementId)

        val water = receipts.first { it.key == ServiceReceiptKey.WATER }
        assertFalse(water.isPaid)
    }

    @Test
    fun `paid receipt exposes actual movement amount without replacing plan reference`() = runBlocking {
        val reference = Money.of(BigDecimal("45.00")).getOrError()
        val actual = Money.of(BigDecimal("55.00")).getOrError()
        planRepo.setPlan(
            FinancialPlan(
                id = "plan-1",
                estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
                fixedExpenses = reference,
                electricityExpenses = reference,
            ),
        )
        movementRepo.setMovements(
            listOf(
                Movement(
                    id = "light-payment",
                    type = MovementType.EXPENSE,
                    amount = actual,
                    categoryId = pe.kipu.core.domain.category.CategoryIds.SERVICES,
                    channel = PaymentChannel.CASH,
                    source = MovementSource.MANUAL,
                    status = MovementStatus.CONFIRMED,
                    recordedAt = now,
                    createdAt = now,
                ),
            ),
        )
        receiptRepo.saveReceipt(
            MonthlyServiceReceipt(
                key = ServiceReceiptKey.LIGHT,
                title = "Luz",
                configuredAmount = reference,
                monthKey = "2026-08",
                isPaid = true,
                paidMovementId = "light-payment",
                paidAt = now,
            ),
        )

        val light = useCase().first().single()

        assertEquals(reference, light.configuredAmount)
        assertEquals(actual, light.paidAmount)
    }

    @Test
    fun `reflects custom fixed expenses and updated configured amounts`() = runBlocking {
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("3000")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("250")).getOrError(),
            electricityExpenses = Money.of(BigDecimal("150")).getOrError(),
            customFixedExpensesJson = "custom-1|Gimnasio|100",
        )
        planRepo.setPlan(plan)

        val receipts = useCase().first()

        assertEquals(2, receipts.size)
        val light = receipts.first { it.key == ServiceReceiptKey.LIGHT }
        assertEquals(Money.of(BigDecimal("150.00")).getOrError(), light.configuredAmount)

        val gym = receipts.first { it.key.type == pe.kipu.core.domain.receipt.ServiceReceiptType.CUSTOM }
        assertEquals("Gimnasio", gym.title)
        assertEquals(Money.of(BigDecimal("100.00")).getOrError(), gym.configuredAmount)
    }

    @Test
    fun `switches receipt observation when month changes in Lima`() = runTest {
        var current = Instant.parse("2026-09-01T04:59:30Z")
        val changingTimeProvider = TimeProvider { current }
        val changingReceiptRepo = FakeMonthlyServiceReceiptRepo()
        val changingUseCase = ObserveMonthlyServiceReceiptsUseCase(
            planRepo,
            changingReceiptRepo,
            movementRepo,
            changingTimeProvider,
        )

        val job = backgroundScope.launch { changingUseCase().collect {} }
        runCurrent()
        assertEquals(listOf("2026-08"), changingReceiptRepo.requestedMonths)

        current = Instant.parse("2026-09-01T05:00:30Z")
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(listOf("2026-08", "2026-09"), changingReceiptRepo.requestedMonths)
        job.cancel()
    }

    private class FakeFinancialPlanRepo : FinancialPlanRepository {
        private val plansFlow = MutableStateFlow<List<FinancialPlan>>(emptyList())

        fun setPlan(plan: FinancialPlan) {
            plansFlow.value = listOf(plan)
        }

        override fun observePlans(): Flow<List<FinancialPlan>> = plansFlow
        override suspend fun getById(id: EntityId): FinancialPlan? = plansFlow.value.find { it.id == id }
        override suspend fun save(plan: FinancialPlan): Result<Unit> {
            plansFlow.value = listOf(plan)
            return Result.success(Unit)
        }
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeMonthlyServiceReceiptRepo : MonthlyServiceReceiptRepository {
        private val receiptsFlow = MutableStateFlow<List<MonthlyServiceReceipt>>(emptyList())
        val requestedMonths = mutableListOf<String>()

        override fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>> {
            requestedMonths += monthKey
            return receiptsFlow
        }

        override fun observeAllPaidMovementIds(): Flow<Set<String>> =
            kotlinx.coroutines.flow.flowOf(emptySet())

        override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) {
            val current = receiptsFlow.value.toMutableList()
            current.removeAll { it.key.identifier == receipt.key.identifier && it.monthKey == receipt.monthKey }
            current.add(receipt)
            receiptsFlow.value = current
        }

        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt? =
            receiptsFlow.value.find { it.monthKey == monthKey && it.key.identifier == serviceKeyIdentifier }
    }

    private class FakeMovementRepo : MovementRepository {
        private val movements = MutableStateFlow<List<Movement>>(emptyList())

        fun setMovements(value: List<Movement>) {
            movements.value = value
        }

        override fun observeMovements(): Flow<List<Movement>> = movements
        override suspend fun getById(id: EntityId): Movement? = movements.value.find { it.id == id }
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }
}
