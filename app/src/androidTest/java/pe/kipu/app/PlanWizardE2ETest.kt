package pe.kipu.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import pe.kipu.app.support.skipOnboardingIfShown
import pe.kipu.app.support.waitForMainNavigation

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PlanWizardE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.skipOnboardingIfShown()
    }

    @Test
    fun planWizardCompletesFromEnvelopesTab() {
        composeRule.waitForMainNavigation()

        composeRule.onNodeWithText("Sobres").performClick()
        composeRule.onNodeWithText("Ingresos").performClick()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                composeRule.onNodeWithText("¿Cuánto dinero recibes?").assertExists()
                composeRule.onNodeWithText("Tengo sueldo fijo").assertExists()
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithText("No sé exacto").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("3500")
        tapContinue()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                composeRule.onNodeWithText("¿Qué pagos sí o sí tienes?").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("No tengo gastos fijos").performScrollTo()
        composeRule.onNodeWithText("No tengo gastos fijos").performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("¿Cuánto quieres gastar a la semana?").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onAllNodesWithText("S/ 80")[0].performScrollTo()
        composeRule.onAllNodesWithText("S/ 80")[0].performClick()
        composeRule.onNodeWithText("S/ 30").performScrollTo()
        composeRule.onNodeWithText("S/ 30").performClick()
        composeRule.onNodeWithText("S/ 40").performScrollTo()
        composeRule.onNodeWithText("S/ 40").performClick()
        composeRule.onNodeWithText("S/ 100").performScrollTo()
        composeRule.onNodeWithText("S/ 100").performClick()
        tapContinue()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("Gastos hormiga").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("S/ 25").performScrollTo()
        composeRule.onNodeWithText("S/ 25").performClick()
        tapContinue()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("¿Qué quieres lograr?").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Saltar meta por ahora").performScrollTo()
        composeRule.onNodeWithText("Saltar meta por ahora").performClick()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                composeRule.onNodeWithText("Tu plan está listo").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNode(hasText("Crear mi plan", substring = true)).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNode(hasText("Crear mi plan", substring = true)).performScrollTo()
        composeRule.onNode(hasText("Crear mi plan", substring = true)).performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("Mis Sobres").assertExists()
                true
            }.getOrDefault(false)
        }
    }

    private fun tapContinue() {
        composeRule.onNode(hasText("Continuar", substring = true)).performScrollTo()
        composeRule.onNode(hasText("Continuar", substring = true)).performClick()
    }
}
