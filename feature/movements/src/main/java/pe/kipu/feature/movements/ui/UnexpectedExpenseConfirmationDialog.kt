package pe.kipu.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.feature.movements.presentation.UnexpectedExpenseConfirmationState

@Composable
fun UnexpectedExpenseConfirmationDialog(
    state: UnexpectedExpenseConfirmationState,
    onAdjustmentToggled: (String) -> Unit,
    onConfirmWithAdjustments: () -> Unit,
    onConfirmWithoutAdjustments: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coverage = state.preview.coverage
    val selectedPlan = state.selectedRecoveryPlan
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text("Revisa esta compra imprevista") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Kipu no moverá tu dinero. Esta es la distribución que quedará registrada.")
                CoverageLine("De tu reserva", coverage.fromReserve.amount)
                CoverageLine("De tu saldo disponible", coverage.fromAvailableBalance.amount)
                CoverageLine("Aún por compensar", coverage.uncovered.amount)

                if (state.preview.recoveryPlan.adjustments.isNotEmpty()) {
                    Text(
                        text = "Reajuste propuesto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Puedes quitar cualquier ajuste. Comida y transporte no se reducen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.preview.recoveryPlan.adjustments.forEach { adjustment ->
                        val selected = adjustment.envelopeId in state.selectedEnvelopeIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = selected,
                                    enabled = !state.isSaving,
                                    role = Role.Checkbox,
                                    onValueChange = { onAdjustmentToggled(adjustment.envelopeId) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = selected, onCheckedChange = null, enabled = !state.isSaving)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(adjustment.envelopeName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Reducir ${formatPenAmountForDisplay(adjustment.reduction.amount)}; nuevo límite " +
                                        formatPenAmountForDisplay(adjustment.proposedLimit.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Text(
                        text = if (selectedPlan.remainingGap.isZero()) {
                            "El reajuste seleccionado cubre el faltante."
                        } else {
                            "Todavía faltará compensar ${formatPenAmountForDisplay(selectedPlan.remainingGap.amount)}."
                        },
                        color = if (selectedPlan.remainingGap.isZero()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    KipuSecondaryButton(
                        text = "Guardar sin reajustar sobres",
                        onClick = onConfirmWithoutAdjustments,
                        enabled = !state.isSaving,
                    )
                }

                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics {
                            error(message)
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = when {
                    state.isSaving -> "Guardando..."
                    state.preview.recoveryPlan.adjustments.isEmpty() -> "Guardar compra"
                    else -> "Guardar y reajustar"
                },
                onClick = if (state.preview.recoveryPlan.adjustments.isEmpty()) {
                    onConfirmWithoutAdjustments
                } else {
                    onConfirmWithAdjustments
                },
                enabled = !state.isSaving,
            )
        },
        dismissButton = {
            KipuDialogDismissButton(
                text = "Volver",
                onClick = onDismiss,
                enabled = !state.isSaving,
            )
        },
    )
}

@Composable
private fun CoverageLine(label: String, amount: java.math.BigDecimal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(formatPenAmountForDisplay(amount), fontWeight = FontWeight.SemiBold)
    }
}
