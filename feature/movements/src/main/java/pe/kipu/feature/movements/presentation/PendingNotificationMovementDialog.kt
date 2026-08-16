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
    enabled: Boolean = true,
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
                    enabled = enabled,
                )
                KipuPrimaryButton(
                    text = "Confirmar",
                    onClick = onConfirm,
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
fun PendingNotificationDuplicateDialog(
    pendingMovement: Movement,
    existingMatch: Movement,
    onResolve: (DuplicateResolution) -> Unit,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
) {
    val summary = buildNotificationDuplicateSummary(pendingMovement, existingMatch)

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) onResolve(DuplicateResolution.CANCEL)
        },
        modifier = modifier,
        title = { Text(text = "Posible duplicado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ya tienes un ingreso similar confirmado. " +
                        "El ingreso por notificación parece repetir el mismo pago.",
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "¿Qué quieres hacer?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = "Fusionar",
                onClick = { onResolve(DuplicateResolution.MERGE) },
                enabled = !isProcessing,
            )
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KipuDialogDismissButton(
                    text = "No es duplicado",
                    onClick = { onResolve(DuplicateResolution.SAVE_AS_NEW) },
                    enabled = !isProcessing,
                )
                KipuDialogDismissButton(
                    text = "Cancelar",
                    onClick = { onResolve(DuplicateResolution.CANCEL) },
                    enabled = !isProcessing,
                )
            }
        },
    )
}

private fun buildNotificationDuplicateSummary(
    pendingMovement: Movement,
    existingMatch: Movement,
): String {
    val pendingTitle = movementDisplayTitle(pendingMovement.counterpartyName, pendingMovement.description)
    val existingTitle = movementDisplayTitle(existingMatch.counterpartyName, existingMatch.description)
    val pendingAmount = formatPenAmountForDisplay(pendingMovement.amount.amount)
    val existingAmount = formatPenAmountForDisplay(existingMatch.amount.amount)
    val pendingDate = formatMovementDate(pendingMovement.recordedAt)
    val existingDate = formatMovementDate(existingMatch.recordedAt)

    return "Notificación: $pendingTitle · $pendingAmount · $pendingDate\n" +
        "Confirmado: $existingTitle · $existingAmount · $existingDate"
}
