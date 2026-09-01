package pe.kipu.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.theme.KipuTheme

/** Secondary outline button — HTML `.btn-secondary`. */
@Composable
fun KipuSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    destructive: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.5.dp,
            if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.6f),
        ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KipuSecondaryButtonPreview() {
    KipuTheme(darkTheme = true) {
        KipuSecondaryButton(
            text = "Descartar",
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
