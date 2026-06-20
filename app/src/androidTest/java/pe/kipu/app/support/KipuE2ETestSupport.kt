package pe.kipu.app.support

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import pe.kipu.app.MainActivity

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.skipOnboardingIfShown() {
    waitUntil(timeoutMillis = 60_000) {
        runCatching {
            onNodeWithText("Inicio").assertExists()
            true
        }.getOrElse {
            runCatching {
                onNodeWithText("Configurar plan después").assertExists()
                true
            }.getOrDefault(false)
        }
    }
    runCatching {
        onNodeWithText("Configurar plan después")
            .performScrollTo()
            .performClick()
        waitForIdle()
    }
    waitUntil(timeoutMillis = 60_000) {
        runCatching {
            onNodeWithText("Inicio").assertExists()
            true
        }.getOrDefault(false)
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
