package pe.kipu.feature.movements.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.feature.movements.presentation.formatMovementDate

@Composable
fun DuplicateResolutionDialog(
    pair: MovementDuplicatePair,
    onResolve: (DuplicateResolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reasonText = MovementDuplicateTranslator.matchReasonText(pair.matchReasonKey)
    val summary = buildDuplicateSummary(pair)

    AlertDialog(
        onDismissRequest = { onResolve(DuplicateResolution.CANCEL) },
        modifier = modifier,
        title = { Text(text = "Posible duplicado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = reasonText)
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "¿Qué quieres hacer con estos movimientos?",
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

private fun buildDuplicateSummary(pair: MovementDuplicatePair): String {
    val firstTitle = movementDisplayTitle(pair.movementA.counterpartyName, pair.movementA.description)
    val secondTitle = movementDisplayTitle(pair.movementB.counterpartyName, pair.movementB.description)
    val firstAmount = formatPenAmountForDisplay(pair.movementA.amount.amount)
    val secondAmount = formatPenAmountForDisplay(pair.movementB.amount.amount)
    val firstDate = formatMovementDate(pair.movementA.recordedAt)
    val secondDate = formatMovementDate(pair.movementB.recordedAt)

    return "$firstTitle · $firstAmount · $firstDate\n$secondTitle · $secondAmount · $secondDate"
}
