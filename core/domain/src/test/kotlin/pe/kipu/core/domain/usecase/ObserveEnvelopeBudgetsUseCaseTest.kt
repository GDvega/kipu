package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator

class ObserveEnvelopeBudgetsUseCaseTest {

    @Test
    fun `emits recalculated budget states when movements change`() = runTest {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val cycleRange = cycleRangeCalculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, )
        val movementInstant = cycleRange.start.plusSeconds(3_600)
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val movement = Movement(
            id = "m1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("50.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = movementInstant,
            createdAt = movementInstant,
        )
        val useCase = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(listOf(envelope)),
            movementRepository = FakeMovementRepository(listOf(movement)),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
            monthlyServiceReceiptRepository = FakeMonthlyServiceReceiptRepository(),
            financialPlanRepository = FakeFinancialPlanRepository(emptyList()),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val states = useCase().first()

        assertEquals(1, states.size)
        assertEquals(50, states.first().percentUsed)
        assertEquals(EnvelopeBudgetStatus.OK, states.first().status)
    }

    @Test
    fun `monthly plan counts spending from earlier in the month, outside the current week`() = runTest {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        // 2026-06-05 cae en el mes actual pero fuera de la semana del 15-21 (lunes-based).
        val earlierInMonth = Instant.parse("2026-06-05T12:00:00Z")
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val movement = Movement(
            id = "m1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("40.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = earlierInMonth,
            createdAt = earlierInMonth,
        )
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = Money.ZERO,
            budgetCycle = BudgetCycle.MONTHLY,
        )
        val useCase = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(listOf(envelope)),
            movementRepository = FakeMovementRepository(listOf(movement)),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
            monthlyServiceReceiptRepository = FakeMonthlyServiceReceiptRepository(),
            financialPlanRepository = FakeFinancialPlanRepository(listOf(plan)),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val states = useCase().first()

        // Con ventana mensual el gasto del 5-jun cuenta (40/100 = 40%); con la semanal sería 0%.
        assertEquals(40, states.first().percentUsed)
    }

    @Test
    fun `monthly fixed service payment does not consume weekly envelope`() = runTest {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val envelope = Envelope(
            id = "envelope-services",
            name = "Servicios corrientes",
            weeklyLimit = Money.of(BigDecimal("60.00")).getOrError(),
            categoryId = CategoryIds.SERVICES,
        )
        // Pago de luz mensual de S/ 150 (gasto fijo mensual)
        val monthlyLightPayment = Movement(
            id = "m-light",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("150.00")).getOrError(),
            categoryId = CategoryIds.SERVICES,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = reference,
            createdAt = reference,
        )
        val receiptRepo = FakeMonthlyServiceReceiptRepository(paidMovementIds = setOf("m-light"))
        val useCase = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(listOf(envelope)),
            movementRepository = FakeMovementRepository(listOf(monthlyLightPayment)),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
            monthlyServiceReceiptRepository = receiptRepo,
            financialPlanRepository = FakeFinancialPlanRepository(emptyList()),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val states = useCase().first()

        // El pago fijo mensual no debe descontar del sobre semanal de servicios (0% usado, no 250%)
        assertEquals(0, states.first().percentUsed)
        assertEquals(Money.ZERO, states.first().spentAmount)
        assertEquals(Money.of(BigDecimal("60.00")).getOrError(), states.first().remainingAmount)
    }

    @Test
    fun `movement assigned to one shared category envelope is counted only once`() = runTest {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val leisure = Envelope(
            id = "envelope-leisure",
            name = "Ocio",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
        )
        val family = Envelope(
            id = "envelope-family",
            name = "Familia",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
        )
        val movement = Movement(
            id = "m-shared-category",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("40.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
            envelopeId = leisure.id,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = reference,
            createdAt = reference,
        )
        val useCase = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(listOf(leisure, family)),
            movementRepository = FakeMovementRepository(listOf(movement)),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
            monthlyServiceReceiptRepository = FakeMonthlyServiceReceiptRepository(),
            financialPlanRepository = FakeFinancialPlanRepository(emptyList()),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val states = useCase().first().associateBy { it.envelopeId }

        assertEquals(Money.of(BigDecimal("40.00")).getOrError(), states.getValue(leisure.id).spentAmount)
        assertEquals(Money.ZERO, states.getValue(family.id).spentAmount)
    }

    private class FakeMonthlyServiceReceiptRepository(
        private val paidMovementIds: Set<String> = emptySet(),
    ) : pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository {
        override fun observeReceiptsForMonth(monthKey: String) = flowOf(emptyList<pe.kipu.core.domain.receipt.MonthlyServiceReceipt>())
        override fun observeAllPaidMovementIds(): Flow<Set<String>> = flowOf(paidMovementIds)
        override suspend fun saveReceipt(receipt: pe.kipu.core.domain.receipt.MonthlyServiceReceipt) {}
        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String) = null
    }

    private class FakeFinancialPlanRepository(
        private val plans: List<FinancialPlan>,
    ) : FinancialPlanRepository {
        override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(plans)
        override suspend fun getById(id: String): FinancialPlan? = plans.find { it.id == id }
        override suspend fun save(plan: FinancialPlan) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeEnvelopeRepository(
        private val envelopes: List<Envelope>,
    ) : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(envelopes)
        override suspend fun getById(id: String): Envelope? = envelopes.find { it.id == id }
        override suspend fun save(envelope: Envelope) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeMovementRepository(
        private val movements: List<Movement>,
    ) : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)
        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName.equals(counterpartyName, ignoreCase = true) }

        override suspend fun save(movement: Movement) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeGatheringExpenseRepository : pe.kipu.core.domain.repository.GatheringExpenseRepository {
        override fun observeTotalsByGathering() = flowOf(emptyMap<pe.kipu.core.domain.model.EntityId, pe.kipu.core.domain.model.Money>())
        override fun observeExpensesByGathering() = flowOf(emptyMap<pe.kipu.core.domain.model.EntityId, List<pe.kipu.core.domain.model.GatheringExpense>>())
        override fun observeLinkedMovementIds() = flowOf(emptySet<pe.kipu.core.domain.model.EntityId>())
        override fun observeActiveGatheringLinkedMovementIds() = flowOf(emptySet<pe.kipu.core.domain.model.EntityId>())
        override suspend fun isMovementLinked(movementId: pe.kipu.core.domain.model.EntityId) = false
        override suspend fun save(expense: pe.kipu.core.domain.model.GatheringExpense) = Result.success(Unit)
    }
}
