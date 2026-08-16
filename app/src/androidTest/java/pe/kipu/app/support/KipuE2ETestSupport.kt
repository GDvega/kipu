package pe.kipu.app.support

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import pe.kipu.app.MainActivity
import pe.kipu.app.navigation.KipuDestination
import pe.kipu.app.presentation.MainViewModel
import pe.kipu.core.designsystem.component.KipuTestTags

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.skipOnboardingIfShown() {
    waitUntil(timeoutMillis = 60_000) {
        nodeWithTextExists("Inicio") ||
            nodeWithTextExists("Comenzar con mi plan") ||
            nodeWithTextExists("¿Cuánto dinero recibes?")
    }

    if (nodeWithTextExists("Inicio")) return

    if (nodeWithTextExists("Comenzar con mi plan") || nodeWithTextExists("¿Cuánto dinero recibes?")) {
        lateinit var mainViewModel: MainViewModel
        runOnIdle {
            mainViewModel = ViewModelProvider(activity)[MainViewModel::class.java]
            mainViewModel.resetOnboarding()
        }
        waitUntil(timeoutMillis = 20_000) {
            nodeWithTextExists("Comenzar con mi plan")
        }
        onNodeWithText("Comenzar con mi plan")
            .performScrollTo()
            .performClick()
        waitUntil(timeoutMillis = 20_000) {
            nodeWithTextExists("¿Cuánto dinero recibes?") && !mainViewModel.pendingPlanWizard.value
        }
        runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        waitUntil(timeoutMillis = 60_000) {
            nodeWithTextExists("Inicio")
        }
        return
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.reachPlanSummaryWithApproximateIncome(
    amount: String,
) {
    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("¿Cuánto dinero recibes?")
    }
    waitUntil(timeoutMillis = 10_000) {
        nodeWithTextExists("No sé exacto")
    }
    tapText("No sé exacto")
    waitForIdle()
    onAllNodes(hasSetTextAction(), useUnmergedTree = true)[1]
        .performScrollTo()
        .performTextReplacement(amount)
    waitForIdle()
    tapButtonContaining("Continuar")

    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("¿Qué pagos sí o sí tienes?")
    }
    tapText("No tengo gastos fijos")

    waitUntil(timeoutMillis = 15_000) {
        nodeWithTextExists("¿Cuánto quieres asignar a tus sobres?")
    }
    tapButtonContaining("Continuar")

    waitUntil(timeoutMillis = 15_000) {
        nodeWithTextExists("Gastos hormiga")
    }
    tapButtonContaining("Continuar")

    waitUntil(timeoutMillis = 15_000) {
        nodeWithTextExists("¿Qué quieres lograr?")
    }
    tapText("Saltar meta por ahora")

    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("Tu plan está listo")
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.openPlanIncomeFromEnvelopes() {
    waitForMainNavigation()
    onNodeWithTag(KipuTestTags.bottomBarTab(KipuDestination.Envelopes.route)).performClick()
    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("Mis Sobres")
    }
    waitUntil(timeoutMillis = 20_000) {
        runCatching {
            onNode(
                hasClickAction() and hasAnyDescendant(hasText("Ingresos")),
                useUnmergedTree = true,
            ).assertExists()
            true
        }.getOrDefault(false)
    }
    tapButtonContaining("Ingresos")
    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("¿Cuánto dinero recibes?")
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForMainNavigation() {
    waitUntil(timeoutMillis = 30_000) {
        runCatching {
            onNodeWithText("Inicio").assertExists()
            true
        }.getOrDefault(false)
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForHomeScreen() {
    waitForMainNavigation()
    runCatching {
        onNodeWithText("Inicio").performClick()
        waitForIdle()
    }
    waitUntil(timeoutMillis = 15_000) {
        runCatching {
            onNodeWithText("Tu dinero protegido").assertExists()
            true
        }.getOrElse {
            runCatching {
                onNodeWithText("Bienvenido a Kipu").assertExists()
                true
            }.getOrDefault(false)
        }
    }
}

private fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.nodeWithTextExists(
    text: String,
): Boolean = runCatching {
    onNodeWithText(text).assertExists()
    true
}.getOrDefault(false)

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.tapButtonContaining(
    text: String,
) {
    val node = onNode(
        hasText(text, substring = true) and hasClickAction(),
    )
    runCatching {
        node.performClick()
    }.getOrElse {
        node.performScrollTo().performClick()
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.tapText(
    text: String,
) {
    val node = onNode(hasText(text) and hasClickAction())
    if (runCatching { node.assertIsDisplayed() }.isFailure) {
        node.performScrollTo()
    }
    node.performClick()
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.tapClickableContainingText(
    text: String,
) {
    val node = onNode(
        hasClickAction() and hasAnyDescendant(hasText(text)),
        useUnmergedTree = true,
    )
    runCatching { node.performClick() }.getOrElse {
        node.performScrollTo().performClick()
    }
}
