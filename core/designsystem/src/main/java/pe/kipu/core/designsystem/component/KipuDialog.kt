package pe.kipu.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard alert dialog using Kipu button styling.
 */
@Composable
fun KipuAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissText: String? = "Cancelar",
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructiveConfirm: Boolean = false,
    textContent: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            when {
                textContent != null -> textContent()
                text != null -> Text(text = text)
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = confirmEnabled,
                destructive = destructiveConfirm,
            )
        },
        dismissButton = if (dismissText != null) {
            {
                KipuDialogDismissButton(
                    text = dismissText,
                    onClick = onDismiss ?: onDismissRequest,
                )
            }
        } else {
            null
        },
    )
}
