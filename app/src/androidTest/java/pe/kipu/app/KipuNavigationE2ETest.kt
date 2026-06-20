package pe.kipu.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class KipuNavigationE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.skipOnboardingIfShown()
    }

    @Test
    fun bottomBarNavigatesToAllMainTabs() {
        composeRule.waitForMainNavigation()

        composeRule.onNodeWithText("Movimientos").performClick()
        composeRule.onNodeWithText("Detectados automáticamente").assertExists()

        composeRule.onNodeWithText("Sobres").performClick()
        composeRule.onNodeWithText("Comida").assertExists()

        composeRule.onNodeWithText("Compromisos").performClick()
        composeRule.onNodeWithText("Fondo emergencia").assertExists()

        composeRule.onNodeWithText("Perfil").performClick()
        composeRule.onNodeWithText("Configuración y preferencias").assertExists()

        composeRule.onNodeWithText("Inicio").performClick()
        composeRule.onNodeWithText("Inicio").assertExists()
    }

    @Test
    fun profileNavigatesToGatherings() {
        composeRule.waitForMainNavigation()

        composeRule.onNodeWithText("Perfil").performClick()
        composeRule.onNodeWithText("Ver juntas").performClick()
        composeRule.onNodeWithText("Gastos compartidos con amigos").assertExists()
        composeRule.onNodeWithText("Nueva junta").assertExists()
    }

    @Test
    fun createGatheringShowsInList() {
        openGatheringsScreen()

        composeRule.onNodeWithText("Nueva junta").performClick()
        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[0]
            .performTextReplacement("Cena E2E")
        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[1]
            .performTextReplacement("Ana\nLuis")
        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("Cena E2E").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Cena E2E").assertIsDisplayed()
        composeRule.onNodeWithText("2 participantes").assertIsDisplayed()
    }

    private fun openGatheringsScreen() {
        composeRule.waitForMainNavigation()
        composeRule.onNodeWithText("Perfil").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("Ver juntas").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Ver juntas").performClick()
        composeRule.onNodeWithText("Gastos compartidos con amigos").assertIsDisplayed()
    }
}
