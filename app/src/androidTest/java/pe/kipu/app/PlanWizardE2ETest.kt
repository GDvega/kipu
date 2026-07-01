package pe.kipu.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.feature.onboarding.ui.PlanIntroStep

@RunWith(AndroidJUnit4::class)
class PlanWizardE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingCtaRequestsPlanWizardStart() {
        var startRequested = false
        composeRule.setContent {
            KipuTheme {
                PlanIntroStep(onStart = { startRequested = true })
            }
        }

        composeRule.onNodeWithText("Comenzar con mi plan").performClick()

        composeRule.runOnIdle {
            assertTrue(startRequested)
        }
    }
}
