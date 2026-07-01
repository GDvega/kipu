package pe.kipu.feature.envelopes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.EnvelopeBudgetState

@Composable
fun EnvelopeAdjustLimitDialog(
    budget: EnvelopeBudgetState,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
) {
    var amountText by rememberSaveable(budget.envelopeId) {
        mutableStateOf(budget.weeklyLimit.amount.stripTrailingZeros().toPlainString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar presupuesto") },
        text = {
            Column {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Actual: ${formatPenAmountForDisplay(budget.weeklyLimit.amount)} por semana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                KipuPenOutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Nuevo límite semanal",
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = "Guardar",
                onClick = { onSave(amountText) },
            )
        },
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
