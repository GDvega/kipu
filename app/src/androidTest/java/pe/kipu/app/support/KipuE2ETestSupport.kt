package pe.kipu.app.support

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.runBlocking
import pe.kipu.app.MainActivity
import pe.kipu.core.data.preferences.KipuPreferencesDataStore
import pe.kipu.core.data.preferences.UserPreferencesKeys

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.skipOnboardingIfShown() {
    waitUntil(timeoutMillis = 60_000) {
        nodeWithTextExists("Inicio") ||
            nodeWithTextExists("Comenzar con mi plan") ||
            nodeWithTextExists("¿Cuánto dinero recibes?")
    }

    if (nodeWithTextExists("Inicio")) return

    if (nodeWithTextExists("Comenzar con mi plan") || nodeWithTextExists("¿Cuánto dinero recibes?")) {
        runBlocking {
            KipuPreferencesDataStore.get(activity).edit { prefs ->
                prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] = true
                prefs[UserPreferencesKeys.PENDING_PLAN_WIZARD] = false
            }
        }
        activityRule.scenario.recreate()
        waitUntil(timeoutMillis = 60_000) {
            nodeWithTextExists("Inicio")
        }
        return
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.completePlanWizardWithDefaults() {
    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("¿Cuánto dinero recibes?")
    }
    waitUntil(timeoutMillis = 10_000) {
        nodeWithTextExists("Sueldo mensual (aproximado)")
    }
    replaceTextFieldContaining("Sueldo mensual (aproximado)", "1500")
    waitForIdle()
    tapButtonContaining("Continuar")

    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("¿Qué pagos sí o sí tienes?")
    }
    tapText("No tengo gastos fijos")

    waitUntil(timeoutMillis = 15_000) {
        nodeWithTextExists("¿Cuánto quieres gastar a la semana?")
    }
    tapButtonContaining("Continuar")

    waitUntil(timeoutMillis = 15_000) {
        nodeWithTextExists("Gastos hormiga")
    }
    tapText("S/ 25")
    tapButtonContaining("Continuar")

    waitUntil(timeoutMillis = 15_000) {
        nodeWithTextExists("¿Qué quieres lograr?")
    }
    tapText("Saltar meta por ahora")

    waitUntil(timeoutMillis = 20_000) {
        nodeWithTextExists("Tu plan está listo")
    }
    tapButtonContaining("mi plan")

    waitForIdle()
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
        hasContentDescription(text, substring = true) and hasClickAction(),
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
    val node = onNodeWithText(text)
    runCatching {
        node.performClick()
    }.getOrElse {
        node.performScrollTo().performClick()
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.tapClickableContainingText(
    text: String,
) {
    val node = onNode(
        hasClickAction() and hasAnyDescendant(hasText(text)),
        useUnmergedTree = true,
    )
    runCatching {
        node.performClick()
    }.getOrElse {
        node.performScrollTo().performClick()
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.replaceTextFieldContaining(
    label: String,
    value: String,
) {
    onNode(
        hasSetTextAction() and hasAnyDescendant(hasText(label)),
        useUnmergedTree = true,
    ).performTextReplacement(value)
}
