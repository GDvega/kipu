package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.duplicate.DuplicateMatchReasonKeys
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.feature.movements.presentation.DuplicateResolutionDialog

@RunWith(AndroidJUnit4::class)
class DuplicateResolutionDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsDuplicateResolutionActionsInSpanish() {
        composeRule.setContent {
            KipuTheme {
                DuplicateResolutionDialog(
                    pair = samplePair(),
                    onResolve = {},
                )
            }
        }

        composeRule.onNodeWithText("Posible duplicado").assertIsDisplayed()
        composeRule.onNodeWithText("Fusionar").assertIsDisplayed()
        composeRule.onNodeWithText("No es duplicado").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    private fun samplePair(): MovementDuplicatePair {
        val recordedAt = Instant.parse("2026-06-16T15:00:00Z")
        val movement = Movement(
            id = "movement-a",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("25.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "Maria",
            recordedAt = recordedAt,
            createdAt = recordedAt,
        )

        return MovementDuplicatePair(
            movementA = movement,
            movementB = movement.copy(id = "movement-b"),
            matchReasonKey = DuplicateMatchReasonKeys.AMOUNT_COUNTERPARTY_TIME,
        )
    }
}
