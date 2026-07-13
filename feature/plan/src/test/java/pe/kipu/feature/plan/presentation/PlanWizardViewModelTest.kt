package pe.kipu.feature.plan.presentation

import androidx.lifecycle.SavedStateHandle
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.PlanSetupRepository
import pe.kipu.core.domain.plan.PreparePlanSetupUseCase
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.usecase.CalculateCategoryPeriodSpentUseCase
import pe.kipu.core.domain.usecase.CalculateCycleAvailableUseCase
import pe.kipu.core.domain.usecase.CalculateEnvelopeBudgetStateUseCase
import pe.kipu.core.domain.usecase.CalculateGoalWeeklyContributionUseCase
import pe.kipu.core.domain.usecase.CalculatePeriodEnvelopeTotalsUseCase
import pe.kipu.core.domain.usecase.CreateCategoryUseCase
import pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCase
import pe.kipu.core.domain.usecase.ObserveEnvelopeBudgetsUseCase
import pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class PlanWizardViewModelTest {
    @Test
    fun `two ViewModels receive different savers and persist concurrently`() = viewModelTest {
        val gateA = CompletableDeferred<Unit>()
        val gateB = CompletableDeferred<Unit>()
        val harnessA = harness(roomGate = gateA)
        val harnessB = harness(roomGate = gateB)

        val saverA = harnessA.saver
        val saverB = harnessB.saver
        assertTrue(saverA !== saverB)
        assertTrue(saverA.isSaving !== saverB.isSaving)
        prepareValidSave(harnessA.viewModel)
        prepareValidSave(harnessB.viewModel)

        var navigationsA = 0
        var navigationsB = 0
        harnessA.viewModel.onFinish { navigationsA += 1 }
        harnessB.viewModel.onFinish { navigationsB += 1 }
        runCurrent()

        assertTrue(harnessA.content().isSaving)
        assertTrue(harnessB.content().isSaving)
        assertEquals(1, harnessA.room.calls)
        assertEquals(1, harnessB.room.calls)

        gateA.complete(Unit)
        gateB.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, navigationsA)
        assertEquals(1, navigationsB)
        assertFalse(harnessA.content().isSaving)
        assertFalse(harnessB.content().isSaving)
    }

    @Test
    fun `complete success clears saving and navigates once`() = viewModelTest {
        val harness = harness()
        prepareValidSave(harness.viewModel)
        var navigations = 0

        harness.viewModel.onFinish { navigations += 1 }
        advanceUntilIdle()

        assertEquals(1, navigations)
        assertFalse(harness.content().isSaving)
        assertEquals(null, harness.content().errorMessage)
    }

    @Test
    fun `DataStore warning remains financial success and navigates once`() = viewModelTest {
        val harness = harness(preferencesResult = Result.failure(IllegalStateException("preferences")))
        prepareValidSave(harness.viewModel)
        var navigations = 0

        harness.viewModel.onFinish { navigations += 1 }
        advanceUntilIdle()

        assertEquals(1, navigations)
        assertFalse(harness.content().isSaving)
        assertTrue(harness.content().errorMessage.orEmpty().contains("plan se guardó"))
        assertEquals(1, harness.room.calls)
    }

    @Test
    fun `preparation failure clears saving without navigation`() = viewModelTest {
        val harness = harness()
        runCurrent()
        harness.viewModel.onAntSpendingLimitChanged("10")
        runCurrent()
        var navigations = 0

        harness.viewModel.onFinish { navigations += 1 }
        advanceUntilIdle()

        assertEquals(0, navigations)
        assertFalse(harness.content().isSaving)
        assertTrue(harness.content().errorMessage.orEmpty().contains("Revisa los datos"))
        assertEquals(0, harness.room.calls)
    }

    @Test
    fun `Room failure clears saving without navigation`() = viewModelTest {
        val harness = harness(roomResult = Result.failure(IllegalStateException("room")))
        prepareValidSave(harness.viewModel)
        var navigations = 0

        harness.viewModel.onFinish { navigations += 1 }
        advanceUntilIdle()

        assertEquals(0, navigations)
        assertFalse(harness.content().isSaving)
        assertEquals("No pudimos guardar tu plan", harness.content().errorMessage)
        assertEquals(0, harness.preferences.updateCalls)
    }

    @Test
    fun `double finish keeps one operation and one navigation`() = viewModelTest {
        val gate = CompletableDeferred<Unit>()
        val harness = harness(roomGate = gate)
        prepareValidSave(harness.viewModel)
        var navigations = 0

        harness.viewModel.onFinish { navigations += 1 }
        harness.viewModel.onFinish { navigations += 1 }
        runCurrent()

        assertTrue(harness.content().isSaving)
        assertEquals(1, harness.room.calls)
        assertEquals(0, navigations)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, navigations)
        assertFalse(harness.content().isSaving)
        assertEquals(1, harness.room.calls)
    }

    @Test
    fun `cancellation clears saving and does not navigate`() = viewModelTest {
        val cancellation = CancellationException("cancel room")
        val harness = harness(roomResult = Result.failure(cancellation))
        prepareValidSave(harness.viewModel)
        var navigations = 0

        harness.viewModel.onFinish { navigations += 1 }
        advanceUntilIdle()

        assertEquals(0, navigations)
        assertFalse(harness.content().isSaving)
        assertEquals(0, harness.preferences.updateCalls)
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun TestScope.prepareValidSave(viewModel: PlanWizardViewModel) {
        runCurrent()
        viewModel.onAntSpendingLimitChanged("10")
        viewModel.onSkipGoal()
        runCurrent()
    }

    private fun harness(
        roomResult: Result<Unit> = Result.success(Unit),
        roomGate: CompletableDeferred<Unit>? = null,
        preferencesResult: Result<Unit> = Result.success(Unit),
    ): Harness {
        val timeProvider = FixedTimeProvider
        val categoryRepository = InMemoryCategoryRepository()
        val envelopeRepository = InMemoryEnvelopeRepository()
        val commitmentRepository = InMemoryCommitmentRepository()
        val financialPlanRepository = InMemoryFinancialPlanRepository()
        val preferences = RecordingPreferencesRepository(preferencesResult)
        val room = RecordingPlanSetupRepository(roomResult, roomGate)
        val saver = PlanWizardSaver(
            preparePlanSetup = PreparePlanSetupUseCase(ValidateFinancialPlanUseCase()),
            planSetupRepository = room,
            userPreferencesRepository = preferences,
        )
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = envelopeRepository,
            movementRepository = EmptyMovementRepository,
            gatheringExpenseRepository = EmptyGatheringExpenseRepository,
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )
        val viewModel = PlanWizardViewModel(
            savedStateHandle = SavedStateHandle(),
            financialPlanRepository = financialPlanRepository,
            commitmentRepository = commitmentRepository,
            categoryRepository = categoryRepository,
            envelopeRepository = envelopeRepository,
            userPreferencesRepository = preferences,
            observeEnvelopeBudgets = observeEnvelopeBudgets,
            planWizardSaver = saver,
            validateFinancialPlan = ValidateFinancialPlanUseCase(),
            calculateCycleAvailable = CalculateCycleAvailableUseCase(
                CalculatePeriodEnvelopeTotalsUseCase(),
            ),
            estimateMonthlyIncome = EstimateMonthlyIncomeUseCase(),
            createCategory = CreateCategoryUseCase(categoryRepository, timeProvider),
            calculateGoalWeeklyContribution = CalculateGoalWeeklyContributionUseCase(),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )
        return Harness(viewModel, saver, room, preferences)
    }

    private data class Harness(
        val viewModel: PlanWizardViewModel,
        val saver: PlanWizardSaver,
        val room: RecordingPlanSetupRepository,
        val preferences: RecordingPreferencesRepository,
    ) {
        fun content(): PlanWizardUiState.Content = viewModel.uiState.value as PlanWizardUiState.Content
    }

    private class RecordingPlanSetupRepository(
        private val result: Result<Unit>,
        private val gate: CompletableDeferred<Unit>?,
    ) : PlanSetupRepository {
        var calls: Int = 0

        override suspend fun save(setup: pe.kipu.core.domain.plan.PlanSetup): Result<Unit> {
            calls += 1
            gate?.await()
            return result
        }
    }

    private class RecordingPreferencesRepository(
        private val updateResult: Result<Unit>,
    ) : UserPreferencesRepository {
        private val state = MutableStateFlow(UserPreferences())
        var updateCalls: Int = 0

        override fun observePreferences(): Flow<UserPreferences> = state

        override suspend fun updatePreferences(
            transform: (UserPreferences) -> UserPreferences,
        ): Result<Unit> {
            updateCalls += 1
            if (updateResult.isSuccess) state.value = transform(state.value)
            return updateResult
        }

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }

    private class InMemoryFinancialPlanRepository : FinancialPlanRepository {
        private val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = money("2000"),
            fixedExpenses = Money.ZERO,
        )

        override fun observePlans(): Flow<List<FinancialPlan>> = MutableStateFlow(listOf(plan))
        override suspend fun getById(id: String): FinancialPlan? = plan.takeIf { it.id == id }
        override suspend fun save(plan: FinancialPlan): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class InMemoryCategoryRepository : CategoryRepository {
        private val state = MutableStateFlow(listOf(Category(CategoryIds.OTHER, "Otros")))

        override fun observeCategories(): Flow<List<Category>> = state
        override suspend fun getById(id: String): Category? = state.value.firstOrNull { it.id == id }
        override suspend fun save(category: Category): Result<Unit> {
            state.value = state.value.filterNot { it.id == category.id } + category
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class InMemoryEnvelopeRepository : EnvelopeRepository {
        private val state = MutableStateFlow<List<Envelope>>(emptyList())

        override fun observeEnvelopes(): Flow<List<Envelope>> = state
        override suspend fun getById(id: String): Envelope? = state.value.firstOrNull { it.id == id }
        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class InMemoryCommitmentRepository : CommitmentRepository {
        private val state = MutableStateFlow<List<Commitment>>(emptyList())

        override fun observeCommitments(): Flow<List<Commitment>> = state
        override suspend fun getById(id: String): Commitment? = state.value.firstOrNull { it.id == id }
        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private object EmptyMovementRepository : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = MutableStateFlow(emptyList())
        override suspend fun getById(id: String): Movement? = null
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private object EmptyGatheringExpenseRepository : GatheringExpenseRepository {
        override fun observeTotalsByGathering(): Flow<Map<String, Money>> = MutableStateFlow(emptyMap())
        override fun observeExpensesByGathering(): Flow<Map<String, List<GatheringExpense>>> =
            MutableStateFlow(emptyMap())
        override fun observeLinkedMovementIds(): Flow<Set<String>> = MutableStateFlow(emptySet())
        override fun observeActiveGatheringLinkedMovementIds(): Flow<Set<String>> =
            MutableStateFlow(emptySet())
        override suspend fun isMovementLinked(movementId: String): Boolean = false
        override suspend fun save(expense: GatheringExpense): Result<Unit> = Result.success(Unit)
    }

    private object FixedTimeProvider : TimeProvider {
        override fun now(): Instant = Instant.parse("2026-07-13T12:00:00Z")
    }

    private companion object {
        fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()
    }
}
