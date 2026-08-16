package pe.kipu.app

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.app.presentation.MainViewModel
import pe.kipu.app.support.openPlanIncomeFromEnvelopes
import pe.kipu.app.support.reachPlanSummaryWithApproximateIncome
import pe.kipu.app.support.tapButtonContaining
import pe.kipu.app.support.tapText
import pe.kipu.app.support.waitForHomeScreen

@RunWith(AndroidJUnit4::class)
class PlanWizardE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun assertInvalidIncomeErrorIsVisibleAndAnnounced() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                composeRule.onNodeWithText("¿Cuánto dinero recibes?").assertExists()
                true
            }.getOrDefault(false)
        }

        composeRule.tapText("No sé exacto")
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[1]
            .performTextReplacement("")
        composeRule.onNodeWithText("¿Tienes plata disponible ahorita?").performScrollTo()
        composeRule.tapButtonContaining("Continuar")

        val message = "Ingresa un monto de ingreso válido"
        val error = composeRule.onNodeWithText(message)
        error.assertIsDisplayed()
        error.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
        error.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                message,
            ),
        )
    }

    @Test
    fun wizardShowsAccessibleErrorAndPersistsEditsThroughAllSixSteps() {
        openPlanWizardFromOnboarding()
        assertInvalidIncomeErrorIsVisibleAndAnnounced()

        composeRule.reachPlanSummaryWithApproximateIncome("5000")
        composeRule.tapButtonContaining("mi plan")
        composeRule.waitForHomeScreen()

        composeRule.openPlanIncomeFromEnvelopes()
        assertApproximateIncome("5000")

        composeRule.reachPlanSummaryWithApproximateIncome("5200")
        composeRule.onNodeWithText("✓ Guardar mi plan", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.tapButtonContaining("Guardar mi plan")
        composeRule.waitForHomeScreen()

        composeRule.openPlanIncomeFromEnvelopes()
        assertApproximateIncome("5200")
    }

    private fun assertApproximateIncome(expected: String) {
        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[1]
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.EditableText,
                    AnnotatedString(expected),
                ),
            )
    }

    private fun openPlanWizardFromOnboarding() {
        composeRule.runOnIdle {
            ViewModelProvider(composeRule.activity)[MainViewModel::class.java]
                .resetOnboarding()
        }

        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                composeRule.onNodeWithText("Comenzar con mi plan").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Comenzar con mi plan")
            .performScrollTo()
            .performClick()
    }
}
