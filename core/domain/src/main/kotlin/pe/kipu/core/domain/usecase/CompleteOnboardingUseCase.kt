package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.repository.UserPreferencesRepository

class CompleteOnboardingUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(pendingPlanWizard: Boolean = false): Result<Unit> =
        userPreferencesRepository.updatePreferences { preferences ->
            preferences.copy(
                onboardingCompleted = true,
                pendingPlanWizard = pendingPlanWizard,
            )
        }
}
