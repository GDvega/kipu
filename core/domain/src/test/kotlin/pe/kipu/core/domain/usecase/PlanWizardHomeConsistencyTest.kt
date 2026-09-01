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
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator

class PlanWizardHomeConsistencyTest {

    @Test
    fun wizardPreviewAndHomeAgreeOnWeeklyCycle() = runTest {
        assertPreviewMatchesHome(BudgetCycle.WEEKLY)
    }

    @Test
    fun wizardPreviewAndHomeAgreeOnMonthlyCycle() = runTest {
        assertPreviewMatchesHome(BudgetCycle.MONTHLY)
    }

    private suspend fun assertPreviewMatchesHome(cycle: BudgetCycle) {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val envelopes = listOf(
            envelope("envelope-food", "Comida", "120.00", CategoryIds.FOOD),
            envelope("envelope-transport", "Transporte", "50.00", CategoryIds.TRANSPORT),
            envelope("envelope-ant-spending", "Gastos hormiga", "35.00", CategoryIds.OTHER),
        )
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
            fixedExpenses = Money.ZERO,
            initialBalance = Money.of(BigDecimal("150.00")).getOrError(),
            budgetCycle = cycle,
        )
        val observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            movementRepository = FakeMovementRepository(),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
            monthlyServiceReceiptRepository = FakeMonthlyServiceReceiptRepository(),
            financialPlanRepository = FakeFinancialPlanRepository(plan),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )
        val calculateCycleAvailable = CalculateCycleAvailableUseCase(
            CalculatePeriodEnvelopeTotalsUseCase(),
        )
        val home = ObserveHomeInsightsUseCase(
            observeEnvelopeBudgets = observeEnvelopeBudgets,
            movementRepository = FakeMovementRepository(),
            commitmentRepository = FakeCommitmentRepository(),
            categoryRepository = FakeCategoryRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            financialPlanRepository = FakeFinancialPlanRepository(plan),
            reserveEventRepository = object : pe.kipu.core.domain.repository.ReserveEventRepository {
                override fun observeAll() = flowOf(emptyList<pe.kipu.core.domain.model.ReserveEvent>())
                override suspend fun getById(id: String): pe.kipu.core.domain.model.ReserveEvent? = null
                override suspend fun record(event: pe.kipu.core.domain.model.ReserveEvent) = Result.success(Unit)
            },
            calculateCycleAvailable = calculateCycleAvailable,
            detectAntSpending = DetectAntSpendingUseCase(),
            detectAntSpendingWeeklyLimitUseCase = DetectAntSpendingWeeklyLimitUseCase(),
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
            calculateCategoryExpenseDistribution = CalculateCategoryExpenseDistributionUseCase(),
            calculateReserveBalance = CalculateReserveBalanceUseCase(),
            calculateAvailableBalance = CalculateAvailableBalanceUseCase(),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val previewBudgets = observeEnvelopeBudgets().first()
        val previewDaily = calculateCycleAvailable(
            previewBudgets,
            reference,
            cycleRangeCalculator.currentCycleRange(cycle, reference),
            cycle,
        )
        val homeInsights = home().first()
        val homeDaily = homeInsights.cycleAvailable
        val homeCash = requireNotNull(homeInsights.cashFlowSummary).netCash

        assertEquals(cycle, homeDaily.cycle)
        assertEquals(previewDaily.cycleAvailable, homeDaily.cycleAvailable)
        assertEquals(previewDaily.cycleRemaining, homeDaily.cycleRemaining)
        assertEquals(previewDaily.daysRemainingInCycle, homeDaily.daysRemainingInCycle)
        assertEquals(BigDecimal("150.00"), homeCash)
    }

    private fun envelope(id: String, name: String, weeklyLimit: String, categoryId: String): Envelope =
        Envelope(
            id = id,
            name = name,
            weeklyLimit = Money.of(BigDecimal(weeklyLimit)).getOrError(),
            categoryId = categoryId,
        )

    private class FakeFinancialPlanRepository(
        private val plan: FinancialPlan,
    ) : FinancialPlanRepository {
        override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(listOf(plan))
        override suspend fun getById(id: String): FinancialPlan? = plan.takeIf { it.id == id }
        override suspend fun save(plan: FinancialPlan): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeEnvelopeRepository(
        private val envelopes: List<Envelope>,
    ) : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(envelopes)
        override suspend fun getById(id: String): Envelope? = envelopes.find { it.id == id }
        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeGatheringExpenseRepository : pe.kipu.core.domain.repository.GatheringExpenseRepository {
        override fun observeTotalsByGathering() = flowOf(emptyMap<pe.kipu.core.domain.model.EntityId, pe.kipu.core.domain.model.Money>())
        override fun observeExpensesByGathering() = flowOf(emptyMap<pe.kipu.core.domain.model.EntityId, List<pe.kipu.core.domain.model.GatheringExpense>>())
        override fun observeLinkedMovementIds() = flowOf(emptySet<pe.kipu.core.domain.model.EntityId>())
        override fun observeActiveGatheringLinkedMovementIds() = flowOf(emptySet<pe.kipu.core.domain.model.EntityId>())
        override suspend fun isMovementLinked(movementId: pe.kipu.core.domain.model.EntityId) = false
        override suspend fun save(expense: pe.kipu.core.domain.model.GatheringExpense) = Result.success(Unit)
    }

    private class FakeMovementRepository : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())
        override suspend fun getById(id: String): Movement? = null
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        override fun observePreferences(): Flow<UserPreferences> = flowOf(UserPreferences())
        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit> =
            Result.success(Unit)
        override suspend fun clear(): Result<Unit> = Result.success(Unit)
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

    private class FakeCategoryRepository : pe.kipu.core.domain.repository.CategoryRepository {
        override fun observeCategories(): Flow<List<pe.kipu.core.domain.model.Category>> = flowOf(emptyList())
        override suspend fun getById(id: String): pe.kipu.core.domain.model.Category? = null
        override suspend fun save(category: pe.kipu.core.domain.model.Category): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
