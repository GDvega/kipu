package pe.kipu.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.feature.onboarding.ui.PlanIntroStep

@RunWith(AndroidJUnit4::class)
class OnboardingCtaVisibilitySmallScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingPrimaryCtaIsVisibleWithoutScrolling() {
        composeRule.setContent {
            KipuTheme {
                Box(modifier = Modifier.requiredSize(width = 320.dp, height = 480.dp)) {
                    PlanIntroStep(onStart = {})
                }
            }
        }

        composeRule.onNodeWithText("Comenzar con mi plan").assertIsDisplayed()
    }
}
