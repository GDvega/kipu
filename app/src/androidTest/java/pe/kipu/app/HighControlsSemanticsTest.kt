package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.component.KipuFilterChip
import pe.kipu.core.designsystem.component.KipuSelectionCard
import pe.kipu.core.designsystem.component.KipuSpeedDialFab
import pe.kipu.core.designsystem.component.SpeedDialAction
import pe.kipu.core.designsystem.theme.KipuTheme

@RunWith(AndroidJUnit4::class)
class HighControlsSemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectedControlsExposeNativeSelectionAndStayEnabled() {
        var chipClicks = 0
        composeRule.setContent {
            KipuTheme {
                Column {
                    KipuFilterChip(
                        text = "Claro",
                        selected = true,
                        onClick = { chipClicks += 1 },
                    )
                    KipuSelectionCard(
                        title = "Ingreso fijo",
                        subtitle = "Recibes un monto estable",
                        selected = true,
                        onClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Claro")
            .assertIsSelected()
            .assertIsEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, chipClicks) }
        composeRule.onNodeWithText("Ingreso fijo")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton,
                ),
            )
    }

    @Test
    fun speedDialBackClosesMenuWithoutLeavingTheScreen() {
        composeRule.setContent {
            KipuTheme {
                var expanded by remember { mutableStateOf(false) }
                KipuSpeedDialFab(
                    actions = listOf(
                        SpeedDialAction(
                            label = "Escanear comprobante",
                            icon = Icons.Default.Add,
                            onClick = {},
                        ),
                    ),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Abrir menú de registro").performClick()
        composeRule.onNodeWithText("Escanear comprobante")
            .assertHeightIsAtLeast(48.dp)
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)

        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Cerrar menú de registro").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Abrir menú de registro").assertExists()
    }

    @Test
    fun lightThemeSemanticPairsMeetContrast() = assertThemeContrast(darkTheme = false)

    @Test
    fun darkThemeSemanticPairsMeetContrast() = assertThemeContrast(darkTheme = true)

    private fun assertThemeContrast(darkTheme: Boolean) {
        lateinit var textPairs: List<Triple<String, Color, Color>>
        lateinit var componentPairs: List<Triple<String, Color, Color>>
        composeRule.setContent {
            KipuTheme(darkTheme = darkTheme) {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    textPairs = listOf(
                        Triple("primary", colors.primary, colors.onPrimary),
                        Triple("primaryContainer", colors.primaryContainer, colors.onPrimaryContainer),
                        Triple("secondary", colors.secondary, colors.onSecondary),
                        Triple("secondaryContainer", colors.secondaryContainer, colors.onSecondaryContainer),
                        Triple("tertiary", colors.tertiary, colors.onTertiary),
                        Triple("tertiaryContainer", colors.tertiaryContainer, colors.onTertiaryContainer),
                        Triple("error", colors.error, colors.onError),
                        Triple("errorContainer", colors.errorContainer, colors.onErrorContainer),
                        Triple("surfaceVariant", colors.surfaceVariant, colors.onSurfaceVariant),
                    )
                    componentPairs = listOf(
                        Triple("outline", colors.surface, colors.outline),
                        Triple("outlineVariant", colors.background, colors.outlineVariant),
                    )
                }
            }
        }

        composeRule.runOnIdle {
            textPairs.forEach { (name, background, foreground) ->
                assertTrue("$name contrast", contrastRatio(background, foreground) >= 4.5f)
            }
            componentPairs.forEach { (name, background, foreground) ->
                assertTrue("$name contrast", contrastRatio(background, foreground) >= 3f)
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
