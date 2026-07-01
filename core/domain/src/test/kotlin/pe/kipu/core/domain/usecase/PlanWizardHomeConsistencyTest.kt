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
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator

class PlanWizardHomeConsistencyTest {

    @Test
    fun wizardPreviewAndHomeUseSameDailyAvailableCalculation() = runTest {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val envelopes = listOf(
            envelope("envelope-food", "Comida", "120.00", CategoryIds.FOOD),
            envelope("envelope-transport", "Transporte", "50.00", CategoryIds.TRANSPORT),
            envelope("envelope-ant-spending", "Gastos hormiga", "35.00", CategoryIds.OTHER),
        )
        val observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            movementRepository = FakeMovementRepository(),
            gatheringExpenseRepository = FakeGatheringExpenseRepository(),
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
            userPreferencesRepository = FakeUserPreferencesRepository(),
            calculateCycleAvailable = calculateCycleAvailable,
            detectAntSpending = DetectAntSpendingUseCase(),
            detectAntSpendingWeeklyLimitUseCase = DetectAntSpendingWeeklyLimitUseCase(),
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val previewBudgets = observeEnvelopeBudgets().first()
        val previewDaily = calculateCycleAvailable(
            previewBudgets,
            reference,
            cycleRangeCalculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, reference),
            pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
        )
        val homeDaily = home().first().cycleAvailable

        assertEquals(previewDaily.cycleAvailable, homeDaily.cycleAvailable)
        assertEquals(previewDaily.cycleRemaining, homeDaily.cycleRemaining)
        assertEquals(previewDaily.daysRemainingInCycle, homeDaily.daysRemainingInCycle)
    }

    private fun envelope(id: String, name: String, weeklyLimit: String, categoryId: String): Envelope =
        Envelope(
            id = id,
            name = name,
            weeklyLimit = Money.of(BigDecimal(weeklyLimit)).getOrError(),
            categoryId = categoryId,
        )

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
}
