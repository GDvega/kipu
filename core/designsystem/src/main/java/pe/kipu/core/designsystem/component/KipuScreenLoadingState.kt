package pe.kipu.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Standard loading pattern: optional header stays visible, spinner centered below.
 */
@Composable
fun KipuScreenLoadingState(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    greeting: String? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (title != null) {
            KipuScreenHeader(
                title = title,
                subtitle = subtitle,
                greeting = greeting,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            KipuLoadingIndicator()
        }
    }
}
