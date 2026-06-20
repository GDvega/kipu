package pe.kipu.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Primary action inside [androidx.compose.material3.AlertDialog] footer slots. */
@Composable
fun KipuDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    KipuPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fillWidth = false,
    )
}

/** Dismiss action inside [androidx.compose.material3.AlertDialog] footer slots. */
@Composable
fun KipuDialogDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    KipuSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fillWidth = false,
    )
}
