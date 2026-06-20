package pe.kipu.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.theme.KipuTheme

/**
 * Generic error placeholder with optional retry action.
 * User-facing copy only; no technical error details.
 */
@Composable
fun KipuErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (retryLabel != null && onRetry != null) {
            KipuSecondaryButton(
                text = retryLabel,
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KipuErrorStatePreview() {
    KipuTheme {
        KipuErrorState(
            title = "No pudimos cargar los datos",
            message = "Revisa tu conexión e inténtalo de nuevo.",
            retryLabel = "Reintentar",
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KipuErrorStateNoRetryPreview() {
    KipuTheme {
        KipuErrorState(
            title = "Algo salió mal",
            message = "Vuelve a intentarlo más tarde.",
        )
    }
}
