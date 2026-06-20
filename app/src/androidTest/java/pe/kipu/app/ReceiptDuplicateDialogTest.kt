package pe.kipu.app

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.feature.receipts.presentation.ReceiptDuplicateDialog

@RunWith(AndroidJUnit4::class)
class ReceiptDuplicateDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsDuplicateResolutionActionsInSpanish() {
        composeRule.setContent {
            KipuTheme {
                ReceiptDuplicateDialog(
                    duplicatePending = sampleDuplicatePending(),
                    onResolve = {},
                )
            }
        }

        composeRule.onNodeWithText("Posible duplicado").assertIsDisplayed()
        composeRule.onNodeWithText("Fusionar").assertIsDisplayed()
        composeRule.onNodeWithText("No es duplicado").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    private fun sampleDuplicatePending(): ConfirmMovementResult.DuplicatePending {
        val recordedAt = Instant.parse("2026-06-16T15:00:00Z")
        val existing = Movement(
            id = "movement-existing",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("25.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.RECEIPT,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "Maria",
            recordedAt = recordedAt,
            createdAt = recordedAt,
        )
        val candidate = existing.copy(id = "movement-new")

        return ConfirmMovementResult.DuplicatePending(
            candidate = candidate,
            matches = listOf(existing),
        )
    }
}
