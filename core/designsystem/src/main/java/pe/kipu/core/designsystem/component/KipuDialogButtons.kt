package pe.kipu.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Primary action inside [androidx.compose.material3.AlertDialog] footer slots. */
@Composable
fun KipuDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    KipuPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier.testTag(KipuTestTags.DIALOG_CONFIRM),
        enabled = enabled,
        fillWidth = false,
        destructive = destructive,
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
        modifier = modifier.testTag(KipuTestTags.DIALOG_DISMISS),
        enabled = enabled,
        fillWidth = false,
    )
}
