package pe.kipu.feature.receipts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DuplicateResolution

@Composable
fun ReceiptDuplicateDialog(
    duplicatePending: ConfirmMovementResult.DuplicatePending,
    onResolve: (DuplicateResolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidateSummary = duplicateMovementSummary(duplicatePending.candidate)
    val existingSummary = duplicatePending.matches.joinToString("\n") { duplicateMovementSummary(it) }

    AlertDialog(
        onDismissRequest = { onResolve(DuplicateResolution.CANCEL) },
        modifier = modifier,
        title = { Text(text = "Posible duplicado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Este comprobante coincide con un movimiento que ya registraste:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Nuevo: $candidateSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Existente:\n$existingSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                )
                KipuDialogDismissButton(
                    text = "Cancelar",
                    onClick = { onResolve(DuplicateResolution.CANCEL) },
                )
            }
        },
    )
}
