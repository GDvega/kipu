package pe.kipu.feature.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.feature.onboarding.presentation.OnboardingUiState
import pe.kipu.feature.onboarding.presentation.OnboardingViewModel
import pe.kipu.feature.onboarding.ui.PlanIntroStep

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        OnboardingUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KipuLoadingIndicator()
            }
        }

        is OnboardingUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            error(state.message)
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                    KipuPrimaryButton(
                        text = "Reintentar",
                        onClick = viewModel::retryOnboarding,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }

        OnboardingUiState.Idle -> {
            PlanIntroStep(
                modifier = modifier.fillMaxSize(),
                onStart = {
                    viewModel.onFinishOnboarding(pendingPlanWizard = true)
                },
            )
        }
    }
}
