package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
import pe.kipu.feature.juntas.LinkMovementDialog

@RunWith(AndroidJUnit4::class)
class GatheringLinkMovementDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun ninthMovementCanBeSelected() {
        val selectedMovementId = mutableStateOf<String?>(null)

        composeRule.setContent {
            KipuTheme {
                LinkMovementDialog(
                    movements = (1..9).map(::movement),
                    selectedMovementId = selectedMovementId.value,
                    paidBy = "Ana",
                    participants = listOf("Ana", "Luis"),
                    errorMessage = null,
                    isSaving = false,
                    onMovementSelected = { selectedMovementId.value = it },
                    onPaidByChanged = {},
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
        composeRule.onNode(
            hasClickAction() and hasAnyDescendant(hasText("Movimiento 9", substring = true)),
            useUnmergedTree = true,
        )
            .performClick()
            .assertIsSelected()
    }

    private fun movement(index: Int): Movement {
        val recordedAt = Instant.parse("2026-08-10T15:00:00Z")
        return Movement(
            id = "movement-$index",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal.valueOf(index.toLong())).getOrError(),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "Movimiento $index",
            recordedAt = recordedAt,
            createdAt = recordedAt,
        )
    }
}
