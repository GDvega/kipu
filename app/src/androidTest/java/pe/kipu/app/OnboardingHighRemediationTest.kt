package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.usecase.CompleteOnboardingUseCase
import pe.kipu.feature.onboarding.OnboardingScreen
import pe.kipu.feature.onboarding.presentation.OnboardingUiState
import pe.kipu.feature.onboarding.presentation.OnboardingViewModel

@RunWith(AndroidJUnit4::class)
class OnboardingHighRemediationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun errorIsAnnouncedAsPoliteError() {
        val repository = RecordingPreferencesRepository(failFirstRequest = true)
        lateinit var viewModel: OnboardingViewModel

        composeRule.runOnIdle {
            viewModel = OnboardingViewModel(CompleteOnboardingUseCase(repository))
            viewModel.onFinishOnboarding(pendingPlanWizard = true)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value is OnboardingUiState.Error
        }
        val message = (viewModel.uiState.value as OnboardingUiState.Error).message

        composeRule.setContent {
            KipuTheme {
                OnboardingScreen(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText(message)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
    }

    @Test
    fun retryRepeatsCompletionWithPendingPlanWizard() {
        val repository = RecordingPreferencesRepository(failFirstRequest = true)
        lateinit var viewModel: OnboardingViewModel

        composeRule.runOnIdle {
            viewModel = OnboardingViewModel(CompleteOnboardingUseCase(repository))
            viewModel.onFinishOnboarding(pendingPlanWizard = true)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value is OnboardingUiState.Error
        }

        composeRule.runOnIdle { viewModel.retryOnboarding() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.requests.size == 2
        }

        assertTrue(repository.requests.all(UserPreferences::pendingPlanWizard))
    }

    @Test
    fun loadingPreventsDuplicateCompletionRequests() {
        val requestGate = CompletableDeferred<Unit>()
        val repository = RecordingPreferencesRepository(requestGate = requestGate)
        lateinit var viewModel: OnboardingViewModel

        composeRule.runOnIdle {
            viewModel = OnboardingViewModel(CompleteOnboardingUseCase(repository))
            viewModel.onFinishOnboarding(pendingPlanWizard = true)
            viewModel.onFinishOnboarding(pendingPlanWizard = true)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.requests.isNotEmpty()
        }

        assertEquals(1, repository.requests.size)
        assertEquals(OnboardingUiState.Loading, viewModel.uiState.value)
        requestGate.complete(Unit)
    }

    private class RecordingPreferencesRepository(
        private val failFirstRequest: Boolean = false,
        private val requestGate: CompletableDeferred<Unit>? = null,
    ) : UserPreferencesRepository {
        val requests = mutableListOf<UserPreferences>()

        override fun observePreferences(): Flow<UserPreferences> = flowOf(UserPreferences())

        override suspend fun updatePreferences(
            transform: (UserPreferences) -> UserPreferences,
        ): Result<Unit> {
            requests += transform(UserPreferences())
            requestGate?.await()
            return if (failFirstRequest && requests.size == 1) {
                Result.failure(IllegalStateException("expected test failure"))
            } else {
                Result.success(Unit)
            }
        }

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }
}
