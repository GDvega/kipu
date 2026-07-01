package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.feature.profile.PrivacyPolicyScreen

@RunWith(AndroidJUnit4::class)
class PrivacyPolicyScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsPrivacyPolicyInSpanish() {
        composeRule.setContent {
            KipuTheme {
                PrivacyPolicyScreen()
            }
        }

        composeRule.onNodeWithText("Política de privacidad").assertIsDisplayed()
        composeRule.onNodeWithText("Notificaciones (opcional)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Exportar y eliminar").performScrollTo().assertIsDisplayed()
    }
}
