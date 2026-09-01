package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.feature.movements.ui.ManualMovementDialog
import pe.kipu.feature.movements.ui.ManualMovementFormState
import pe.kipu.feature.juntas.LinkMovementDialog

@RunWith(AndroidJUnit4::class)
class MediumDialogBehaviorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun confirmButtonDoesNotAnnounceSuccessBeforeTheActionFinishes() {
        var actionCalls = 0
        val hapticTypes = mutableListOf<HapticFeedbackType>()

        composeRule.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                        hapticTypes += hapticFeedbackType
                    }
                },
            ) {
                KipuTheme {
                    KipuDialogConfirmButton(
                        text = "Guardar",
                        onClick = { actionCalls += 1 },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.runOnIdle {
            assertEquals(1, actionCalls)
            assertEquals(emptyList<HapticFeedbackType>(), hapticTypes)
        }
    }

    @Test
    fun savingDialogCannotBeDismissedWithBack() {
        var dismissCalls = 0
        composeRule.setContent {
            KipuTheme {
                ManualMovementDialog(
                    categories = emptyList(),
                    formState = ManualMovementFormState(isSaving = true),
                    onMovementTypeSelected = {},
                    onChannelSelected = {},
                    onAmountChanged = {},
                    onCategorySelected = {},
                    onDescriptionChanged = {},
                    onCounterpartyChanged = {},
                    onConfirm = {},
                    onDismiss = { dismissCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Registrar Gasto").assertIsDisplayed()
        composeRule.onNodeWithText("Monto").assertIsNotEnabled()
        pressBack()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(0, dismissCalls) }
    }

    @Test
    fun savingBottomSheetCannotBeDismissedWithBack() {
        var dismissCalls = 0
        composeRule.setContent {
            KipuTheme {
                LinkMovementDialog(
                    movements = emptyList(),
                    selectedMovementId = null,
                    paidBy = "",
                    participants = emptyList(),
                    errorMessage = null,
                    isSaving = true,
                    onMovementSelected = {},
                    onPaidByChanged = {},
                    onConfirm = {},
                    onDismiss = { dismissCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Vincular movimiento").assertIsDisplayed()
        pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Vincular movimiento").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, dismissCalls) }
    }
}
