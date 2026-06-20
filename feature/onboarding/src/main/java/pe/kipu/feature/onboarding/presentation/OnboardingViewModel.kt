package pe.kipu.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import pe.kipu.core.domain.usecase.CompleteOnboardingUseCase

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    fun onSkipOrFinishDemo() {
        finishOnboarding(pendingPlanWizard = false)
    }

    fun onFinishOnboarding(pendingPlanWizard: Boolean = false) {
        finishOnboarding(pendingPlanWizard = pendingPlanWizard)
    }

    private fun finishOnboarding(pendingPlanWizard: Boolean) {
        viewModelScope.launch {
            completeOnboarding(pendingPlanWizard = pendingPlanWizard)
        }
    }
}
