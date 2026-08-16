@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.feature.profile.PreferenceSwitchRow
import pe.kipu.feature.profile.ProfileStatusMessage
import pe.kipu.feature.profile.presentation.ProfileStatus

@RunWith(AndroidJUnit4::class)
class MediumProfileAccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun preferenceRowExposesOneLabeledSwitchTarget() {
        var enabled = false
        composeRule.setContent {
            KipuTheme {
                PreferenceSwitchRow(
                    title = "Notificaciones de ingresos",
                    subtitle = "Desactivado",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
            }
        }

        composeRule.onNode(hasText("Notificaciones de ingresos") and hasClickAction())
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .performClick()

        composeRule.runOnIdle { assertTrue(enabled) }
    }

    @Test
    fun profileStatusAnnouncesSuccessAndIdentifiesErrors() {
        val successMessage = "Exportación lista para compartir."
        val errorMessage = "No pudimos exportar tus datos."
        composeRule.setContent {
            KipuTheme {
                Column {
                    ProfileStatusMessage(ProfileStatus.Success(successMessage))
                    ProfileStatusMessage(ProfileStatus.Error(errorMessage))
                }
            }
        }

        composeRule.onNode(hasText(successMessage))
            .assert(liveRegion)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        composeRule.onNode(hasText(errorMessage))
            .assert(liveRegion)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))
    }

    private val liveRegion = SemanticsMatcher.expectValue(
        SemanticsProperties.LiveRegion,
        LiveRegionMode.Polite,
    )
}
