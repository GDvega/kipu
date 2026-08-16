package pe.kipu.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.kipu.core.domain.usecase.CompleteOnboardingUseCase

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onFinishOnboarding(pendingPlanWizard: Boolean = false) {
        finishOnboarding(pendingPlanWizard = pendingPlanWizard)
    }

    fun retryOnboarding() {
        finishOnboarding(pendingPlanWizard = true)
    }

    private fun finishOnboarding(pendingPlanWizard: Boolean) {
        if (_uiState.value == OnboardingUiState.Loading) return
        _uiState.value = OnboardingUiState.Loading
        viewModelScope.launch {
            completeOnboarding(pendingPlanWizard = pendingPlanWizard)
                .onFailure {
                    _uiState.value = OnboardingUiState.Error("No pudimos completar el inicio")
                }
        }
    }
}
