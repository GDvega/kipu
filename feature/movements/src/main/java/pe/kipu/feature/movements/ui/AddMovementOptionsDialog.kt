package pe.kipu.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.domain.model.PaymentChannel

@Composable
fun AddMovementOptionsDialog(
    onRegisterManual: (PaymentChannel) -> Unit,
    onRegisterReceipt: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cómo quieres registrar?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "En Perú muchos gastos son en efectivo. También puedes usar un comprobante de Yape o Plin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KipuPrimaryButton(
                    text = "En efectivo",
                    onClick = { onRegisterManual(PaymentChannel.CASH) },
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                )
                KipuSecondaryButton(
                    text = "Otro canal (Yape, Plin, banco)",
                    onClick = { onRegisterManual(PaymentChannel.OTHER) },
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                )
                KipuSecondaryButton(
                    text = "Desde comprobante",
                    onClick = onRegisterReceipt,
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
