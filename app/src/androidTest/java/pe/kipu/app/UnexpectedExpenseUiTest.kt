package pe.kipu.app

import androidx.compose.ui.semantics.Role
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
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

    @Test
    fun longCoverageLabelsLeaveAmountsReadableOnOneLine() {
        val preview = UnexpectedExpensePreview(
            coverage = UnexpectedExpenseCoverage(
                fromReserve = Money.ZERO, fromAvailableBalance = Money.ZERO, uncovered = money("300"),
                isFullyCovered = false, protectedObligations = money("133.37"), liquidityGap = money("88.88"),
            ),
            recoveryPlan = UnexpectedExpenseRecoveryPlan(emptyList(), money("300"), false),
        )
        composeRule.setContent {
            KipuTheme {
                UnexpectedExpenseConfirmationDialog(
                    state = UnexpectedExpenseConfirmationState(
                        ManualMovementFormState(amountText = "300", categoryId = CategoryIds.OTHER), preview,
                    ),
                    onAdjustmentToggled = {}, onConfirmWithAdjustments = {},
                    onConfirmWithoutAdjustments = {}, onDismiss = {},
                )
            }
        }
        listOf("133.37", "88.88").forEach { amount ->
            val results = mutableListOf<TextLayoutResult>()
            composeRule.onNode(hasText(formatPenAmountForDisplay(BigDecimal(amount))))
                .performScrollTo().assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertReadableAmount(results.single())
        }
    }

    @Test
    fun voiceCoverageKeepsAmountsReadableBesideLongLabels() {
        val intent = pe.kipu.core.domain.voice.VoiceFinancialIntent.Expense(
            rawText = "Gasté 300 soles en un microondas", amount = money("300"),
            categoryId = CategoryIds.OTHER, description = "Microondas",
            channel = pe.kipu.core.domain.model.PaymentChannel.CASH,
        )
        val preview = UnexpectedExpensePreview(
            coverage = UnexpectedExpenseCoverage(
                Money.ZERO, Money.ZERO, money("300"), false,
                protectedObligations = money("133.37"), liquidityGap = money("88.88"),
            ),
            recoveryPlan = UnexpectedExpenseRecoveryPlan(emptyList(), money("300"), false),
        )
        composeRule.setContent {
            KipuTheme {
                pe.kipu.feature.home.ui.VoiceConfirmationBottomSheet(
                    voiceState = pe.kipu.core.designsystem.voice.VoiceSpeechState.Success(intent.rawText),
                    parsedIntent = intent, isAnalyzing = false, isSaving = false, saveError = null,
                    unexpectedExpenseState = pe.kipu.feature.home.presentation.VoiceUnexpectedExpenseState(intent, preview),
                    onStartListening = {}, onConfirmIntent = { _, _ -> }, onDismiss = {},
                )
            }
        }
        listOf("133.37", "88.88").forEach { amount ->
            val results = mutableListOf<TextLayoutResult>()
            composeRule.onNode(hasText(formatPenAmountForDisplay(BigDecimal(amount))))
                .performScrollTo().assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertReadableAmount(results.single())
        }
    }

    @Test
    fun readabilityCheckRejectsAnActuallyClippedAmount() {
        val amount = formatPenAmountForDisplay(BigDecimal("133.37"))
        composeRule.setContent {
            KipuTheme { Text(amount, modifier = Modifier.width(1.dp), softWrap = false) }
        }
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNode(hasText(amount)).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertThrows(AssertionError::class.java) { assertReadableAmount(results.single()) }
    }

    private fun assertReadableAmount(layout: TextLayoutResult) {
        assertEquals(1, layout.lineCount)
        assertTrue("Amount is vertically clipped", !layout.didOverflowHeight)
        assertTrue("Amount is ellipsized", !layout.isLineEllipsized(0))
        assertEquals(layout.layoutInput.text.length, layout.getLineEnd(0, visibleEnd = true))
        // Simple Text semantics rebuild MultiParagraph with the parent's maximum width,
        // but retain the measured text size. Compare actual glyphs, not paragraph width.
        layout.layoutInput.text.indices.forEach { offset ->
            val bounds = layout.getBoundingBox(offset)
            assertTrue(
                "Character $offset is clipped: $bounds outside ${layout.size}",
                bounds.left >= 0f && bounds.right <= layout.size.width,
            )
        }
    }
}
