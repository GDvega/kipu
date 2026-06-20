package pe.kipu.feature.movements.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Movement

@Composable
fun PendingNotificationIncomeCard(
    movement: Movement,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = movementDisplayTitle(movement.counterpartyName, movement.description)
    val channelLabel = NotificationMovementTranslator.channelLabel(movement.channel)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "$channelLabel · ${formatMovementDate(movement.recordedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            KipuAmountText(
                amount = movement.amount.amount,
                type = AmountType.INCOME,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KipuSecondaryButton(
                    text = "Descartar",
                    onClick = onDismiss,
                )
                KipuPrimaryButton(
                    text = "Confirmar",
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
fun PendingNotificationDuplicateDialog(
    state: PendingNotificationConfirmState,
    onResolve: (DuplicateResolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountText = state.duplicateMatches.firstOrNull()?.amount?.amount?.let { formatPenAmountForDisplay(it) }
        ?: ""

    AlertDialog(
        onDismissRequest = { onResolve(DuplicateResolution.CANCEL) },
        modifier = modifier,
        title = { Text(text = "Posible duplicado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ya tienes un ingreso similar confirmado ($amountText). " +
                        "¿Quieres guardarlo igual?",
                )
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = "Guardar igual",
                onClick = { onResolve(DuplicateResolution.SAVE_AS_NEW) },
            )
        },
        dismissButton = {
            KipuDialogDismissButton(
                text = "Cancelar",
                onClick = { onResolve(DuplicateResolution.CANCEL) },
            )
        },
    )
}
