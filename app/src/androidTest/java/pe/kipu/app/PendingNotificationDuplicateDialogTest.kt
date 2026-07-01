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
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.feature.movements.presentation.PendingNotificationDuplicateDialog

@RunWith(AndroidJUnit4::class)
class PendingNotificationDuplicateDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsNotificationDuplicateActionsInSpanish() {
        val recordedAt = Instant.parse("2026-06-16T15:00:00Z")
        val pending = sampleMovement(
            id = "movement-pending",
            status = MovementStatus.PENDING_CONFIRMATION,
            source = MovementSource.NOTIFICATION,
            recordedAt = recordedAt,
        )
        val confirmed = pending.copy(
            id = "movement-confirmed",
            status = MovementStatus.CONFIRMED,
            source = MovementSource.MANUAL,
        )

        composeRule.setContent {
            KipuTheme {
                PendingNotificationDuplicateDialog(
                    pendingMovement = pending,
                    existingMatch = confirmed,
                    onResolve = {},
                )
            }
        }

        composeRule.onNodeWithText("Posible duplicado").assertIsDisplayed()
        composeRule.onNodeWithText("Fusionar").assertIsDisplayed()
        composeRule.onNodeWithText("No es duplicado").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    private fun sampleMovement(
        id: String,
        status: MovementStatus,
        source: MovementSource,
        recordedAt: Instant,
    ): Movement = Movement(
        id = id,
        type = MovementType.INCOME,
        amount = Money.of(BigDecimal("50.00")).getOrError(),
        categoryId = CategoryIds.OTHER,
        channel = PaymentChannel.YAPE,
        source = source,
        status = status,
        counterpartyName = "MARIA GARCIA RIOS",
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )
}
