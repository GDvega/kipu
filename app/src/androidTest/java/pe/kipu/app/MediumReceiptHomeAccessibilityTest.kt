@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.component.KipuSpeedDialFab
import pe.kipu.core.designsystem.component.SpeedDialAction
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.model.Category
import pe.kipu.feature.home.UserCategoriesRow
import pe.kipu.feature.receipts.ReceiptReviewErrorText
import pe.kipu.feature.receipts.ReceiptReviewResultContent

@RunWith(AndroidJUnit4::class)
class MediumReceiptHomeAccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun receiptResultStaysVisibleAndAnnouncesSuccessUntilDone() {
        var finished = 0
        composeRule.setContent {
            KipuTheme {
                ReceiptReviewResultContent(
                    duplicateMerged = false,
                    onFinished = { finished += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Movimiento guardado").assertExists()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        ).assertExists()
        composeRule.runOnIdle { assertEquals(0, finished) }

        composeRule.onNodeWithText("Listo").performClick()
        composeRule.runOnIdle { assertEquals(1, finished) }
    }

    @Test
    fun mergedDuplicateExplainsThatNoSecondMovementWasCreated() {
        composeRule.setContent {
            KipuTheme {
                ReceiptReviewResultContent(
                    duplicateMerged = true,
                    onFinished = {},
                )
            }
        }

        composeRule.onNodeWithText("Comprobante revisado").assertExists()
        composeRule.onNodeWithText("Ya existía un movimiento igual. No se creó un duplicado.")
            .assertExists()
        composeRule.onNodeWithText("Listo").assertExists()
    }

    @Test
    fun receiptConfirmationErrorIsAnnouncedAndIdentifiedAsError() {
        val message = "No pudimos completar la acción. Intenta de nuevo."
        composeRule.setContent {
            KipuTheme {
                ReceiptReviewErrorText(message)
            }
        }

        composeRule.onNodeWithText(message)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
    }

    @Test
    fun expandedSpeedDialUsesNativeModalAndBackDismissesIt() {
        composeRule.setContent {
            KipuTheme {
                var expanded by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(false)
                }
                Box(Modifier.fillMaxSize()) {
                    Text("Contenido de fondo")
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
        }

        composeRule.onNodeWithContentDescription("Abrir menú de registro").performClick()
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("Escanear comprobante")
            .assert(hasAnyAncestor(isDialog()))

        pressBack()
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Abrir menú de registro").assertExists()
    }

    @Test
    fun homeCategoriesAreNavigationButtonsWithoutSelectionState() {
        var selectedCategory: String? = null
        composeRule.setContent {
            KipuTheme {
                UserCategoriesRow(
                    categories = listOf(Category(id = "transport", name = "Transporte")),
                    onCategoryClick = { selectedCategory = it },
                )
            }
        }

        composeRule.onNodeWithText("Transporte")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()

        composeRule.runOnIdle { assertEquals("transport", selectedCategory) }
    }
}
