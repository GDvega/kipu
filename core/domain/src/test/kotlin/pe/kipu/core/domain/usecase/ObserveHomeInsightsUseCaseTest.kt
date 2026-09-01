package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator

class ObserveHomeInsightsUseCaseTest {

    private val peruZone: ZoneId = CycleRangeCalculator.PERU_ZONE
    private val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()

    @Test
    fun `combines daily available and ant spending alerts`() = runTest {
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val movements = listOf(
            movement("m1", "5.00", hoursAgo = 1),
            movement("m2", "6.00", hoursAgo = 2),
            movement("m3", "7.00", hoursAgo = 3),
            movement("m4", "50.00", hoursAgo = 4),
        )
        val useCase = createUseCase(
            envelopes = listOf(envelope),
            movements = movements,
            reference = wednesday,
        )

        val insights = useCase().first()

        assertEquals(1, insights.envelopeCount)
        assertEquals(4, insights.movementCount)
        assertEquals(Money.of(BigDecimal("6.40")).getOrError(), insights.cycleAvailable.cycleAvailable)
        // Envelope is healthy (68% < 80% ADJUSTED threshold) so ant alerts are suppressed
        assertTrue(insights.antSpendingAlerts.isEmpty())
    }

    @Test
    fun `still emits ant spending alerts when envelope is exceeded`() = runTest {
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("15.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val movements = listOf(
            movement("m1", "5.00", hoursAgo = 1),
            movement("m2", "6.00", hoursAgo = 2),
            movement("m3", "7.00", hoursAgo = 3),
        )
        val useCase = createUseCase(
            envelopes = listOf(envelope),
            movements = movements,
            reference = wednesday,
        )

        val insights = useCase().first()

        // Envelope is EXCEEDED (18/15 = 120%) so ant alerts still fire
        assertEquals(1, insights.antSpendingAlerts.size)
        assertEquals(AlertSeverity.RED, insights.antSpendingAlerts.first().severity)
    }

    @Test
    fun `returns empty ant alerts when only isolated small expenses exist`() = runTest {
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val useCase = createUseCase(
            envelopes = listOf(envelope),
            movements = listOf(movement("m1", "5.00", hoursAgo = 1)),
            reference = wednesday,
        )

        val insights = useCase().first()

        assertTrue(insights.antSpendingAlerts.isEmpty())
        assertEquals(Money.of(BigDecimal("19.00")).getOrError(), insights.cycleAvailable.cycleAvailable)
    }

    @Test
    fun `derives budget cycle from the plan, not the never-written preference`() = runTest {
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = Money.ZERO,
            budgetCycle = BudgetCycle.MONTHLY,
        )
        val useCase = createUseCase(
            envelopes = listOf(envelope),
            movements = listOf(movement("m1", "5.00", hoursAgo = 1)),
            reference = wednesday,
            plan = plan,
        )

        val insights = useCase().first()

        // El ciclo sale del plan (MENSUAL), no del preferences.budgetCycle fantasma (que sería SEMANAL).
        assertEquals(BudgetCycle.MONTHLY, insights.cycleAvailable.cycle)
    }

    @Test
    fun `derives ant spending alert configuration from the plan instead of DataStore`() = runTest {
        val antEnvelope = Envelope(
            id = pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds.ANT_SPENDING,
            name = "Gastos hormiga",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = Money.ZERO,
            antSpendingLimit = Money.of(BigDecimal("100.00")).getOrError(),
            antSpendingAlertEnabled = false,
            antSpendingAlertPercent = 80,
            antSpendingTrackedCategoryIds = setOf(CategoryIds.FOOD),
        )
        val legacyPreferences = UserPreferences(
            antSpendingWeeklyLimitCents = 10_000L,
            antSpendingAlertEnabled = true,
            antSpendingAlertPercent = 80,
        )
        val useCase = createUseCase(
            envelopes = listOf(antEnvelope),
            movements = listOf(movement("m1", "85.00", hoursAgo = 1)),
            reference = wednesday,
            plan = plan,
            preferences = legacyPreferences,
        )

        val insights = useCase().first()

        assertTrue(insights.antSpendingAlerts.isEmpty())
        assertEquals(plan, insights.financialPlan)
    }

    @Test
    fun `propagates financial plan into insights when present`() = runTest {
        val plan = FinancialPlan(
            id = "plan-monthly-test",
            estimatedMonthlyIncome = Money.of(BigDecimal("1500.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("500.00")).getOrError(),
            budgetCycle = BudgetCycle.MONTHLY,
        )
        val useCase = createUseCase(
            envelopes = emptyList(),
            movements = emptyList(),
            reference = wednesday,
            plan = plan,
        )

        val insights = useCase().first()

        assertEquals(plan, insights.financialPlan)
        assertEquals(BudgetCycle.MONTHLY, insights.cycleAvailable.cycle)
    }

    @Test
    fun `monthly budget subtracts actual expenses in Lima regardless of selected cycle`() = runTest {
        val plan = FinancialPlan(
            id = "plan-monthly-summary",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("45.00")).getOrError(),
            budgetCycle = BudgetCycle.DAILY,
        )
        val currentLightPayment = movement("light-payment", "55.00", hoursAgo = 1)
        val currentCoffee = movement("coffee", "10.00", hoursAgo = 2)
        val previousMonthExpense = movement("old", "100.00", hoursAgo = 3).copy(
            recordedAt = wednesday.minusSeconds(40L * 24 * 60 * 60),
        )
        val useCase = createUseCase(
            envelopes = emptyList(),
            movements = listOf(currentLightPayment, currentCoffee, previousMonthExpense),
            reference = wednesday,
            plan = plan,
        )

        val summary = useCase().first().monthlyBudgetSummary

        assertEquals(Money.of(BigDecimal("2000.00")).getOrError(), summary?.plannedIncome)
        assertEquals(Money.of(BigDecimal("65.00")).getOrError(), summary?.actualExpenses)
        assertEquals(Money.of(BigDecimal("1935.00")).getOrError(), summary?.remaining)
        assertEquals(false, summary?.isOverBudget)
    }

    @Test
    fun `home separates accumulated reserve from accumulated available cash`() = runTest {
        val plan = FinancialPlan(
            id = "plan-reserve",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = Money.ZERO,
            initialBalance = Money.of(BigDecimal("1000.00")).getOrError(),
            reserveMonthlyContribution = Money.of(BigDecimal("200.00")).getOrError(),
        )
        val contribution = ReserveEvent(
            id = "reserve-august",
            type = ReserveEventType.CONTRIBUTION,
            amount = Money.of(BigDecimal("200.00")).getOrError(),
            occurredAt = wednesday,
            createdAt = wednesday,
        )

        val insights = createUseCase(
            envelopes = emptyList(),
            movements = emptyList(),
            reference = wednesday,
            plan = plan,
            reserveEvents = listOf(contribution),
        )().first()

        assertEquals(BigDecimal("200.00"), insights.reserveBalance?.balance)
        assertEquals(BigDecimal("800.00"), insights.availableBalance?.availableBalance)
    }

    private fun createUseCase(
        envelopes: List<Envelope>,
        movements: List<Movement>,
        reference: Instant,
        plan: FinancialPlan? = null,
        preferences: UserPreferences = UserPreferences(),
        reserveEvents: List<ReserveEvent> = emptyList(),
    ): ObserveHomeInsightsUseCase {
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            movementRepository = FakeMovementRepository(movements),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
            monthlyServiceReceiptRepository = FakeMonthlyServiceReceiptRepository(),
            financialPlanRepository = FakeFinancialPlanRepository(plan),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )
        return ObserveHomeInsightsUseCase(
            observeEnvelopeBudgets = observeEnvelopeBudgets,
            movementRepository = FakeMovementRepository(movements),
            commitmentRepository = FakeCommitmentRepository(),
            categoryRepository = FakeCategoryRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(preferences),
            financialPlanRepository = FakeFinancialPlanRepository(plan),
            reserveEventRepository = FakeReserveEventRepository(reserveEvents),
            calculateCycleAvailable = CalculateCycleAvailableUseCase(
                CalculatePeriodEnvelopeTotalsUseCase(),
            ),
            detectAntSpending = DetectAntSpendingUseCase(),
            detectAntSpendingWeeklyLimitUseCase = DetectAntSpendingWeeklyLimitUseCase(),
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
            calculateCategoryExpenseDistribution = CalculateCategoryExpenseDistributionUseCase(),
            calculateReserveBalance = CalculateReserveBalanceUseCase(),
            calculateAvailableBalance = CalculateAvailableBalanceUseCase(),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )
    }

    private class FakeCategoryRepository : pe.kipu.core.domain.repository.CategoryRepository {
        override fun observeCategories(): Flow<List<pe.kipu.core.domain.model.Category>> = flowOf(emptyList())
        override suspend fun getById(id: String): pe.kipu.core.domain.model.Category? = null
        override suspend fun save(category: pe.kipu.core.domain.model.Category): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private fun movement(
        id: String,
        amount: String,
        hoursAgo: Long,
    ): Movement {
        val recordedAt = wednesday.minusSeconds(hoursAgo * 3_600)
        return Movement(
            id = id,
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal(amount)).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = recordedAt,
            createdAt = recordedAt,
        )
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

    private class FakeUserPreferencesRepository(
        private val preferences: UserPreferences,
    ) : UserPreferencesRepository {
        override fun observePreferences(): Flow<UserPreferences> = flowOf(preferences)
        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) = Result.success(Unit)
        override suspend fun clear() = Result.success(Unit)
    }

    private class FakeFinancialPlanRepository(
        private val plan: FinancialPlan?,
    ) : FinancialPlanRepository {
        override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(listOfNotNull(plan))
        override suspend fun getById(id: String): FinancialPlan? = plan?.takeIf { it.id == id }
        override suspend fun save(plan: FinancialPlan) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeReserveEventRepository(
        private val events: List<ReserveEvent>,
    ) : ReserveEventRepository {
        override fun observeAll(): Flow<List<ReserveEvent>> = flowOf(events)
        override suspend fun getById(id: String): ReserveEvent? = events.find { it.id == id }
        override suspend fun record(event: ReserveEvent): Result<Unit> = Result.success(Unit)
    }

    private class FakeGatheringExpenseRepository : pe.kipu.core.domain.repository.GatheringExpenseRepository {
        override fun observeTotalsByGathering() = flowOf(emptyMap<pe.kipu.core.domain.model.EntityId, pe.kipu.core.domain.model.Money>())
        override fun observeExpensesByGathering() = flowOf(emptyMap<pe.kipu.core.domain.model.EntityId, List<pe.kipu.core.domain.model.GatheringExpense>>())
        override fun observeLinkedMovementIds() = flowOf(emptySet<pe.kipu.core.domain.model.EntityId>())
        override fun observeActiveGatheringLinkedMovementIds() = flowOf(emptySet<pe.kipu.core.domain.model.EntityId>())
        override suspend fun isMovementLinked(movementId: pe.kipu.core.domain.model.EntityId) = false
        override suspend fun save(expense: pe.kipu.core.domain.model.GatheringExpense) = Result.success(Unit)
    }

    private class FakeCommitmentRepository : pe.kipu.core.domain.repository.CommitmentRepository {
        override fun observeCommitments() = flowOf(emptyList<pe.kipu.core.domain.model.Commitment>())
        override suspend fun getById(id: String) = null
        override suspend fun save(commitment: pe.kipu.core.domain.model.Commitment) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeMonthlyServiceReceiptRepository : pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository {
        override fun observeReceiptsForMonth(monthKey: String) = flowOf(emptyList<pe.kipu.core.domain.receipt.MonthlyServiceReceipt>())
        override fun observeAllPaidMovementIds(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun saveReceipt(receipt: pe.kipu.core.domain.receipt.MonthlyServiceReceipt) {}
        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String) = null
    }
}
