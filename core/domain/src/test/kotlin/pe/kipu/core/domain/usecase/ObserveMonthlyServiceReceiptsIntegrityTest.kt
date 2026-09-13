package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
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

class ObserveMonthlyServiceReceiptsIntegrityTest {
    private val now = Instant.parse("2026-08-16T15:00:00Z")
    private val reference = Money.of(BigDecimal("45.00")).getOrError()
    private val actual = Money.of(BigDecimal("55.00")).getOrError()
    private val payment = Movement(
        id = "light-payment",
        type = MovementType.EXPENSE,
        amount = actual,
        categoryId = CategoryIds.SERVICES,
        channel = PaymentChannel.CASH,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        recordedAt = now,
        createdAt = now,
    )
    private val savedReceipt = MonthlyServiceReceipt(
        key = ServiceReceiptKey.LIGHT,
        title = "Luz",
        configuredAmount = reference,
        monthKey = "2026-08",
        isPaid = true,
        paidMovementId = payment.id,
        paidAt = now,
    )

    @Test
    fun `deleted payment cannot leave a service displayed as paid`() = runTest {
        val receipt = observeReceipt(emptyList())

        assertPending(receipt)
    }

    @Test
    fun `payment changed to income cannot settle a service`() = runTest {
        val receipt = observeReceipt(listOf(payment.copy(type = MovementType.INCOME)))

        assertPending(receipt)
    }

    @Test
    fun `unconfirmed receipt movement cannot settle a service`() = runTest {
        val receipt = observeReceipt(
            listOf(
                payment.copy(
                    status = MovementStatus.PENDING_CONFIRMATION,
                    source = MovementSource.RECEIPT,
                ),
            ),
        )

        assertPending(receipt)
    }

    @Test
    fun `payment moved to another month cannot settle the original month`() = runTest {
        val receipt = observeReceipt(
            listOf(payment.copy(recordedAt = Instant.parse("2026-07-16T15:00:00Z"))),
        )

        assertPending(receipt)
    }

    @Test
    fun `valid payment displays actual amount and preserves the plan reference`() = runTest {
        val receipt = observeReceipt(listOf(payment))

        assertTrue(receipt.isPaid)
        assertEquals(payment.id, receipt.paidMovementId)
        assertEquals(now, receipt.paidAt)
        assertEquals(actual, receipt.paidAmount)
        assertEquals(reference, receipt.configuredAmount)
    }

    @Test
    fun `payment month uses Lima date even when UTC is already next month`() = runTest {
        val lastSecondOfAugustInLima = Instant.parse("2026-09-01T04:59:59Z")
        val receipt = observeReceipt(
            listOf(payment.copy(recordedAt = lastSecondOfAugustInLima)),
            currentTime = lastSecondOfAugustInLima,
        )

        assertTrue(receipt.isPaid)
        assertEquals(actual, receipt.paidAmount)
        assertEquals("2026-08", receipt.monthKey)
    }

    private fun assertPending(receipt: MonthlyServiceReceipt) {
        assertFalse(receipt.isPaid)
        assertNull(receipt.paidMovementId)
        assertNull(receipt.paidAt)
        assertNull(receipt.paidAmount)
        assertEquals(reference, receipt.configuredAmount)
    }

    private suspend fun observeReceipt(
        movements: List<Movement>,
        currentTime: Instant = now,
    ): MonthlyServiceReceipt {
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = reference,
            electricityExpenses = reference,
        )
        return ObserveMonthlyServiceReceiptsUseCase(
            FakePlanRepository(plan),
            FakeReceiptRepository(savedReceipt),
            FakeMovementRepository(movements),
            TimeProvider { currentTime },
        )().first().single()
    }

    private class FakePlanRepository(private val plan: FinancialPlan) : FinancialPlanRepository {
        override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(listOf(plan))
        override suspend fun getById(id: EntityId): FinancialPlan? = plan.takeIf { it.id == id }
        override suspend fun save(plan: FinancialPlan): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeReceiptRepository(receipt: MonthlyServiceReceipt) : MonthlyServiceReceiptRepository {
        private val receipts = MutableStateFlow(listOf(receipt))

        override fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>> =
            flowOf(receipts.value.filter { it.monthKey == monthKey })

        override fun observeAllPaidMovementIds(): Flow<Set<String>> =
            flowOf(receipts.value.filter { it.isPaid }.mapNotNull { it.paidMovementId }.toSet())

        override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) {
            receipts.value = receipts.value.filterNot {
                it.monthKey == receipt.monthKey && it.key == receipt.key
            } + receipt
        }

        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt? =
            receipts.value.find { it.monthKey == monthKey && it.key.identifier == serviceKeyIdentifier }
    }

    private class FakeMovementRepository(private val movements: List<Movement>) : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)
        override suspend fun getById(id: EntityId): Movement? = movements.find { it.id == id }
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName == counterpartyName }

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }
}
