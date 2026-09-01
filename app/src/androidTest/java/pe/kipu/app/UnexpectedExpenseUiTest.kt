package pe.kipu.app

import androidx.compose.ui.semantics.Role
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.RecoveryEnvelopeAdjustment
import pe.kipu.core.domain.model.UnexpectedExpenseCoverage
import pe.kipu.core.domain.model.UnexpectedExpensePreview
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.getOrError
import pe.kipu.feature.movements.presentation.UnexpectedExpenseConfirmationState
import pe.kipu.feature.movements.ui.ManualMovementDialog
import pe.kipu.feature.movements.ui.ManualMovementFormState
import pe.kipu.feature.movements.ui.UnexpectedExpenseConfirmationDialog

class UnexpectedExpenseUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manualExpenseRequiresExplicitUnexpectedToggle() {
        var selected = false
        composeRule.setContent {
            KipuTheme {
                ManualMovementDialog(
                    categories = listOf(Category(CategoryIds.OTHER, "Otros")),
                    formState = ManualMovementFormState(
                        amountText = "300",
                        categoryId = CategoryIds.OTHER,
                    ),
                    onMovementTypeSelected = {},
                    onChannelSelected = {},
                    onAmountChanged = {},
                    onCategorySelected = {},
                    onDescriptionChanged = {},
                    onCounterpartyChanged = {},
                    onUnexpectedExpenseChanged = { selected = it },
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        val switch = composeRule.onNode(
            hasText("Es una compra imprevista") and
                hasClickAction() and
                SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, Role.Switch),
            useUnmergedTree = false,
        )
        switch.assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertTrue(selected) }
    }

    @Test
    fun confirmationShowsCoverageAndKeepsAdjustmentOptional() {
        var toggledId: String? = null
        var confirmed = false
        val preview = UnexpectedExpensePreview(
            coverage = UnexpectedExpenseCoverage(
                fromReserve = money("100.00"),
                fromAvailableBalance = money("100.00"),
                uncovered = money("100.00"),
                isFullyCovered = false,
            ),
            recoveryPlan = UnexpectedExpenseRecoveryPlan(
                adjustments = listOf(
                    RecoveryEnvelopeAdjustment(
                        envelopeId = "envelope-leisure",
                        envelopeName = "Ocio",
                        currentLimit = money("100.00"),
                        spentAmount = money("20.00"),
                        proposedLimit = money("50.00"),
                        reduction = money("50.00"),
                    ),
                ),
                remainingGap = money("50.00"),
                isFullyRecoverable = false,
            ),
        )
        composeRule.setContent {
            KipuTheme {
                UnexpectedExpenseConfirmationDialog(
                    state = UnexpectedExpenseConfirmationState(
                        form = ManualMovementFormState(amountText = "300", categoryId = CategoryIds.OTHER),
                        preview = preview,
                    ),
                    onAdjustmentToggled = { toggledId = it },
                    onConfirmWithAdjustments = { confirmed = true },
                    onConfirmWithoutAdjustments = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(hasText("De tu reserva")).assertIsDisplayed()
        composeRule.onNode(hasText("Aún por compensar")).assertIsDisplayed()
        composeRule.onNode(
            hasText("Ocio") and hasClickAction(),
            useUnmergedTree = false,
        ).assertIsOn().performClick()
        composeRule.onNode(hasText("Guardar y reajustar")).performClick()

        composeRule.runOnIdle {
            assertEquals("envelope-leisure", toggledId)
            assertTrue(confirmed)
        }
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()
}
