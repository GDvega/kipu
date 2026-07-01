package pe.kipu.feature.onboarding.presentation

sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState

    data object Loading : OnboardingUiState

    data class Error(val message: String) : OnboardingUiState
}
