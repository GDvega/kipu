package pe.kipu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tactile, non-flat icon container with rounded squircle geometry,
 * soft translucent background and vibrant foreground vector tint.
 */
@Composable
fun KipuIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = tint.copy(alpha = 0.14f),
    borderColor: Color = tint.copy(alpha = 0.25f),
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 13.dp,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
