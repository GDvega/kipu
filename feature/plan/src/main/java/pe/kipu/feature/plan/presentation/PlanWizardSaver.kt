package pe.kipu.feature.plan.presentation

import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.plan.PlanSetup
import pe.kipu.core.domain.plan.PlanSetupPreparationError
import pe.kipu.core.domain.plan.PlanSetupPreparationInput
import pe.kipu.core.domain.plan.PlanSetupPreparationResult
import pe.kipu.core.domain.plan.PlanSetupRepository
import pe.kipu.core.domain.plan.PreparePlanSetupUseCase
import pe.kipu.core.domain.repository.UserPreferencesRepository

data class PlanWizardSaveRequest(
    val preparationInput: PlanSetupPreparationInput,
    val antSpendingWeeklyLimitCents: Long?,
    val antSpendingAlertEnabled: Boolean,
    val antSpendingTrackedCategories: Set<String>,
)

sealed interface PlanWizardSaveResult {
    data class Success(
        val setup: PlanSetup,
        val validation: FinancialPlanValidationResult,
    ) : PlanWizardSaveResult

    data class SuccessWithWarning(
        val setup: PlanSetup,
        val validation: FinancialPlanValidationResult,
        val cause: Throwable,
    ) : PlanWizardSaveResult

    data class PreparationFailure(
        val reason: PlanSetupPreparationError,
    ) : PlanWizardSaveResult

    data class PersistenceFailure(val cause: Throwable) : PlanWizardSaveResult

    data object AlreadyInProgress : PlanWizardSaveResult
}

val PlanWizardSaveResult.shouldNavigate: Boolean
    get() = this is PlanWizardSaveResult.Success ||
        this is PlanWizardSaveResult.SuccessWithWarning

class PlanWizardSaver @Inject constructor(
    private val preparePlanSetup: PreparePlanSetupUseCase,
    private val planSetupRepository: PlanSetupRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val saveMutex = Mutex()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    suspend fun save(request: PlanWizardSaveRequest): PlanWizardSaveResult {
        if (!saveMutex.tryLock()) return PlanWizardSaveResult.AlreadyInProgress
        _isSaving.value = true
        try {
            val prepared = when (val result = preparePlanSetup(request.preparationInput)) {
                is PlanSetupPreparationResult.Error -> {
                    return PlanWizardSaveResult.PreparationFailure(result.reason)
                }

                is PlanSetupPreparationResult.Success -> result
            }

            val roomResult = callAsResult { planSetupRepository.save(prepared.setup) }
            roomResult.cancellationOrNull()?.let { throw it }
            roomResult.exceptionOrNull()?.let { failure ->
                return PlanWizardSaveResult.PersistenceFailure(failure)
            }

            val preferencesResult = callAsResult {
                userPreferencesRepository.updatePreferences { preferences ->
                    preferences.withWizardPreferences(request)
                }
            }
            preferencesResult.cancellationOrNull()?.let { throw it }
            preferencesResult.exceptionOrNull()?.let { failure ->
                return PlanWizardSaveResult.SuccessWithWarning(
                    setup = prepared.setup,
                    validation = prepared.validation,
                    cause = failure,
                )
            }

            return PlanWizardSaveResult.Success(
                setup = prepared.setup,
                validation = prepared.validation,
            )
        } finally {
            _isSaving.value = false
            saveMutex.unlock()
        }
    }

    private suspend fun callAsResult(block: suspend () -> Result<Unit>): Result<Unit> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Result.failure(failure)
        }

    private fun Result<Unit>.cancellationOrNull(): CancellationException? =
        exceptionOrNull() as? CancellationException

    private fun UserPreferences.withWizardPreferences(
        request: PlanWizardSaveRequest,
    ): UserPreferences = copy(
        antSpendingWeeklyLimitCents = request.antSpendingWeeklyLimitCents,
        antSpendingAlertEnabled = request.antSpendingAlertEnabled,
        antSpendingAlertPercent = 80,
        antSpendingTrackedCategories = request.antSpendingTrackedCategories,
    )
}
