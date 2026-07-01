package pe.kipu.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import pe.kipu.app.navigation.KipuDestination
import pe.kipu.app.support.skipOnboardingIfShown
import pe.kipu.app.support.tapClickableContainingText
import pe.kipu.app.support.tapText
import pe.kipu.app.support.waitForHomeScreen
import pe.kipu.app.support.waitForMainNavigation
import pe.kipu.core.designsystem.component.KipuTestTags

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
        composeRule.onNodeWithText("Yape, Plin, efectivo y más").assertExists()

        composeRule.onNodeWithText("Sobres").performClick()
        composeRule.onNodeWithText("Mis Sobres").assertExists()

        composeRule.onNodeWithText("Compromisos").performClick()
        composeRule.onNodeWithText("Sin compromisos").assertExists()

        composeRule.onNodeWithText("Perfil").performClick()
        composeRule.onNodeWithText("Configuración y preferencias").assertExists()

        composeRule.onNodeWithText("Inicio").performClick()
        composeRule.onNodeWithText("Inicio").assertExists()
    }

    @Test
    fun createManualMovementFromHomeStillAllowsReturningToHome() {
        composeRule.waitForHomeScreen()

        composeRule.onNodeWithTag(KipuTestTags.REGISTER_FAB).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onAllNodes(hasText("Monto"))[0].assertExists()
                true
            }.getOrDefault(false)
        }

        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[0]
            .performTextReplacement("50")
        composeRule.onNodeWithText("Comida").performClick()
        composeRule.onNodeWithTag(KipuTestTags.DIALOG_CONFIRM).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(KipuTestTags.DIALOG_CONFIRM).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithTag(KipuTestTags.bottomBarTab(KipuDestination.Home.route)).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("Tu dinero protegido").assertExists()
                true
            }.getOrElse {
                runCatching {
                    composeRule.onNodeWithText("Bienvenido a Kipu").assertExists()
                    true
                }.getOrDefault(false)
            }
        }
    }

    @Test
    fun profileNavigatesToPrivacyPolicy() {
        composeRule.waitForMainNavigation()

        composeRule.onNodeWithText("Perfil").performClick()
        composeRule.onNodeWithText("Política de privacidad")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("Cómo Kipu trata tus datos en el dispositivo").assertExists()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun profileNavigatesToGatherings() {
        composeRule.waitForMainNavigation()

        composeRule.onNodeWithText("Perfil").performClick()
        composeRule.onNodeWithText("Ver cuentas compartidas").performClick()
        composeRule.onNodeWithText("Gastos compartidos con amigos").assertExists()
        composeRule.onNodeWithText("Nueva cuenta").assertExists()
    }

    @Test
    fun createGatheringShowsInList() {
        openGatheringsScreen()

        composeRule.onNodeWithText("Nueva cuenta").performClick()
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
                composeRule.onNodeWithText("Ver cuentas compartidas").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Ver cuentas compartidas").performClick()
        composeRule.onNodeWithText("Gastos compartidos con amigos").assertIsDisplayed()
    }
}
