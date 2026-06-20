package pe.kipu.core.designsystem.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.kipu.core.designsystem.theme.KipuPrimary

@Composable
fun KipuLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = KipuPrimary,
    )
}
