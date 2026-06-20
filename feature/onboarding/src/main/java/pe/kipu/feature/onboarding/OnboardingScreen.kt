package pe.kipu.feature.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import pe.kipu.feature.onboarding.presentation.OnboardingViewModel
import pe.kipu.feature.onboarding.ui.PlanIntroStep

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    PlanIntroStep(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        onStart = {
            viewModel.onFinishOnboarding(pendingPlanWizard = true)
        },
        onSkip = viewModel::onSkipOrFinishDemo,
    )
}
