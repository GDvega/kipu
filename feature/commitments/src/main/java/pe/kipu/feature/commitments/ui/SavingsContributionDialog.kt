package pe.kipu.feature.commitments.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import pe.kipu.core.designsystem.component.KipuAmountPresetRow
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.feature.commitments.presentation.SavingsContributionState

@Composable
fun SavingsContributionDialog(
    state: SavingsContributionState,
    onIsDepositChanged: (Boolean) -> Unit,
    onAmountChanged: (String) -> Unit,
    onPresetSelected: (BigDecimal) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = listOf(
        BigDecimal("20"),
        BigDecimal("50"),
        BigDecimal("100"),
        BigDecimal("200"),
    )
    val selectedPreset = state.amountText.toBigDecimalOrNull()

    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = {
            Text(
                text = if (state.isDeposit) "Aportar a ${state.commitmentTitle}" else "Retirar de ${state.commitmentTitle}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ahorro actual: S/ ${state.currentAmount.amount.stripTrailingZeros().toPlainString()}" +
                        (state.targetAmount?.let { " de S/ ${it.amount.stripTrailingZeros().toPlainString()}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                KipuFilterChipRow(
                    labels = listOf("Abonar (+)", "Retirar (-)"),
                    selectedIndex = if (state.isDeposit) 0 else 1,
                    onSelected = { index -> onIsDepositChanged(index == 0) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (state.isDeposit) "Monto a aportar" else "Monto a retirar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )

                KipuAmountPresetRow(
                    presets = presets,
                    selectedAmount = selectedPreset,
                    onPresetSelected = onPresetSelected,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .selectableGroup(),
                )

                KipuPenOutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChanged,
                    label = "Monto",
                    placeholder = "50",
                    modifier = Modifier.padding(top = 12.dp),
                )

                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (state.isSaving) "Guardando..." else if (state.isDeposit) "Confirmar abono" else "Confirmar retiro",
                onClick = onConfirm,
                enabled = !state.isSaving && state.amountText.isNotBlank(),
            )
        },
        dismissButton = {
            KipuDialogDismissButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !state.isSaving,
            )
        },
    )
}
