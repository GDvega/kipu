package pe.kipu.feature.plan.presentation

import java.math.BigDecimal
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.CommitmentIds
import pe.kipu.core.domain.plan.PlanSetup
import pe.kipu.core.domain.plan.PlanSetupPreparationInput
import pe.kipu.core.domain.plan.PlanSetupRepository
import pe.kipu.core.domain.plan.PlanWizardLineItem
import pe.kipu.core.domain.plan.PreparePlanSetupUseCase
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalCoroutinesApi::class)
class PlanWizardSaverTest {
    @Test
    fun `independent saver instances persist concurrently without sharing a global lock`() = runTest {
        val gateA = CompletableDeferred<Unit>()
        val gateB = CompletableDeferred<Unit>()
        val roomA = RecordingPlanSetupRepository(gate = gateA)
        val roomB = RecordingPlanSetupRepository(gate = gateB)
        val saverA = saver(room = roomA)
        val saverB = saver(room = roomB)

        assertTrue(saverA !== saverB)
        assertTrue(saverA.isSaving !== saverB.isSaving)
        assertFalse(
            "PlanWizardSaver must remain unscoped so Hilt creates one per ViewModel",
            PlanWizardSaver::class.java.isAnnotationPresent(javax.inject.Singleton::class.java),
        )

        val first = async { saverA.save(request()) }
        val second = async { saverB.save(request()) }
        runCurrent()

        assertTrue(saverA.isSaving.value)
        assertTrue(saverB.isSaving.value)
        assertEquals(1, roomA.calls)
        assertEquals(1, roomB.calls)

        gateA.complete(Unit)
        gateB.complete(Unit)

        assertTrue(first.await() is PlanWizardSaveResult.Success)
        assertTrue(second.await() is PlanWizardSaveResult.Success)
        assertFalse(saverA.isSaving.value)
        assertFalse(saverB.isSaving.value)
    }

    @Test
    fun `valid preparation sends the same setup instance to Room`() = runTest {
        val room = RecordingPlanSetupRepository()
        val saver = saver(room = room)

        val result = saver.save(request())

        assertTrue(result is PlanWizardSaveResult.Success)
        assertSame(room.saved.single(), (result as PlanWizardSaveResult.Success).setup)
    }

    @Test
    fun `preparation error writes neither Room nor DataStore`() = runTest {
        val room = RecordingPlanSetupRepository()
        val preferences = RecordingPreferencesRepository()
        val saver = saver(room, preferences)

        val result = saver.save(request(input = validInput().copy(antSpendingLimitText = "invalid")))

        assertTrue(result is PlanWizardSaveResult.PreparationFailure)
        assertTrue(room.saved.isEmpty())
        assertEquals(0, preferences.updateCalls)
    }

    @Test
    fun `Room failure does not update DataStore`() = runTest {
        val roomFailure = IllegalStateException("room failed")
        val room = RecordingPlanSetupRepository(result = Result.failure(roomFailure))
        val preferences = RecordingPreferencesRepository()

        val result = saver(room, preferences).save(request())

        assertSame(roomFailure, (result as PlanWizardSaveResult.PersistenceFailure).cause)
        assertEquals(0, preferences.updateCalls)
    }

    @Test
    fun `Room and DataStore success produce financial success`() = runTest {
        val room = RecordingPlanSetupRepository()
        val preferences = RecordingPreferencesRepository()

        val result = saver(room, preferences).save(request())

        assertTrue(result is PlanWizardSaveResult.Success)
        assertEquals(1, room.calls)
        assertEquals(1, preferences.updateCalls)
        assertTrue(result.shouldNavigate)
    }

    @Test
    fun `DataStore failure is financial success with warning and does not retry Room`() = runTest {
        val preferenceFailure = IllegalStateException("preferences failed")
        val room = RecordingPlanSetupRepository()
        val preferences = RecordingPreferencesRepository(result = Result.failure(preferenceFailure))

        val result = saver(room, preferences).save(request())

        assertSame(preferenceFailure, (result as PlanWizardSaveResult.SuccessWithWarning).cause)
        assertEquals(1, room.calls)
        assertEquals(1, preferences.updateCalls)
        assertTrue(result.shouldNavigate)
    }

    @Test
    fun `Room cancellation is preserved and DataStore is untouched`() = runTest {
        val cancellation = CancellationException("cancel room")
        val room = RecordingPlanSetupRepository(result = Result.failure(cancellation))
        val preferences = RecordingPreferencesRepository()

        val thrown = runCatching { saver(room, preferences).save(request()) }.exceptionOrNull()

        assertSame(cancellation, thrown)
        assertEquals(0, preferences.updateCalls)
    }

    @Test
    fun `DataStore cancellation is preserved without retrying Room`() = runTest {
        val cancellation = CancellationException("cancel preferences")
        val room = RecordingPlanSetupRepository()
        val preferences = RecordingPreferencesRepository(result = Result.failure(cancellation))

        val thrown = runCatching { saver(room, preferences).save(request()) }.exceptionOrNull()

        assertSame(cancellation, thrown)
        assertEquals(1, room.calls)
    }

    @Test
    fun `custom prepared identities reach Room without regeneration`() = runTest {
        val room = RecordingPlanSetupRepository()
        val customInput = validInput().copy(
            customEnvelopeLines = listOf(
                PlanWizardLineItem(
                    id = "envelope-custom-pet",
                    label = "Mascota",
                    amountText = "25",
                    categoryId = "category-custom-pet",
                ),
            ),
            existingCategories = validInput().existingCategories +
                Category("category-custom-pet", "Mascota"),
        )

        saver(room = room).save(request(input = customInput))

        val customEnvelope = room.saved.single().envelopes.single { it.id == "envelope-custom-pet" }
        assertEquals("category-custom-pet", customEnvelope.categoryId)
    }

    @Test
    fun `skipped existing goal is expressed as commitment identity to settle`() = runTest {
        val room = RecordingPlanSetupRepository()
        val input = validInput().copy(
            existingCommitments = listOf(
                Commitment(
                    id = CommitmentIds.EMERGENCY_FUND,
                    type = CommitmentType.SAVINGS_GOAL,
                    title = "Fondo",
                    targetAmount = money("500"),
                    isSettled = false,
                ),
            ),
        )

        saver(room = room).save(request(input = input))

        assertEquals(setOf(CommitmentIds.EMERGENCY_FUND), room.saved.single().commitmentIdsToSettle)
    }

    @Test
    fun `concurrent saves keep one operation active and one navigation result`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val room = RecordingPlanSetupRepository(gate = gate)
        val saver = saver(room = room)

        val first = async { saver.save(request()) }
        runCurrent()
        assertTrue(saver.isSaving.value)
        val second = async { saver.save(request()) }
        runCurrent()

        assertTrue(second.await() is PlanWizardSaveResult.AlreadyInProgress)
        assertEquals(1, room.calls)
        gate.complete(Unit)
        val results = listOf(first.await(), second.await())

        assertEquals(1, results.count { it.shouldNavigate })
        assertFalse(saver.isSaving.value)
    }

    private fun saver(
        room: RecordingPlanSetupRepository = RecordingPlanSetupRepository(),
        preferences: RecordingPreferencesRepository = RecordingPreferencesRepository(),
    ): PlanWizardSaver = PlanWizardSaver(
        preparePlanSetup = PreparePlanSetupUseCase(ValidateFinancialPlanUseCase()),
        planSetupRepository = room,
        userPreferencesRepository = preferences,
    )

    private fun request(input: PlanSetupPreparationInput = validInput()): PlanWizardSaveRequest =
        PlanWizardSaveRequest(
            preparationInput = input,
            antSpendingWeeklyLimitCents = 1_000L,
            antSpendingAlertEnabled = true,
            antSpendingTrackedCategories = setOf(CategoryIds.FOOD),
        )

    private fun validInput(): PlanSetupPreparationInput = PlanSetupPreparationInput(
        estimatedMonthlyIncome = money("2000"),
        fixedExpenses = Money.ZERO,
        envelopeLimits = emptyMap(),
        antSpendingLimitText = "10",
        goalSkipped = true,
        goalTitle = "",
        goalTargetText = "",
        goalCurrentText = "",
        goalMonthsText = "5",
        existingCategories = listOf(Category(CategoryIds.OTHER, "Otros")),
    )

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()

    private class RecordingPlanSetupRepository(
        private val result: Result<Unit> = Result.success(Unit),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : PlanSetupRepository {
        val saved = mutableListOf<PlanSetup>()
        var calls: Int = 0

        override suspend fun save(setup: PlanSetup): Result<Unit> {
            calls += 1
            saved += setup
            gate?.await()
            return result
        }
    }

    private class RecordingPreferencesRepository(
        private val result: Result<Unit> = Result.success(Unit),
    ) : UserPreferencesRepository {
        private val preferences = MutableStateFlow(UserPreferences())
        var updateCalls: Int = 0

        override fun observePreferences(): Flow<UserPreferences> = preferences

        override suspend fun updatePreferences(
            transform: (UserPreferences) -> UserPreferences,
        ): Result<Unit> {
            updateCalls += 1
            if (result.isSuccess) preferences.value = transform(preferences.value)
            return result
        }

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }
}
