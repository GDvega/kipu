package pe.kipu.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Agrega una barra de desplazamiento (scrollbar) sutil y elegante a un LazyListState.
 * La barra aparece de forma suave al desplazarse y se desvanece automáticamente al detenerse.
 */
fun Modifier.kipuScrollbar(
    state: LazyListState,
    color: Color? = null,
    thickness: Dp = 4.dp,
    padding: Dp = 3.dp,
    minThumbLength: Dp = 28.dp,
): Modifier = composed {
    val defaultColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val isScrolling = state.isScrollInProgress

    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isScrolling) 150 else 600,
            delayMillis = if (isScrolling) 0 else 500,
        ),
        label = "lazyScrollbarAlpha",
    )

    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo

        if (totalItems > 0 && visibleItems.isNotEmpty() && (totalItems > visibleItems.size || state.canScrollBackward || state.canScrollForward) && alpha > 0f) {
            val viewportHeight = size.height
            val firstVisible = visibleItems.first()

            val thumbHeightRatio = (visibleItems.size.toFloat() / totalItems.toFloat()).coerceIn(0.12f, 0.9f)
            val thumbHeight = (viewportHeight * thumbHeightRatio).coerceAtLeast(minThumbLength.toPx())

            val maxFirstIndex = (totalItems - visibleItems.size).coerceAtLeast(1).toFloat()
            val scrollProgress = (firstVisible.index.toFloat() / maxFirstIndex).coerceIn(0f, 1f)
            val thumbOffset = scrollProgress * (viewportHeight - thumbHeight)

            val thicknessPx = thickness.toPx()
            val paddingPx = padding.toPx()

            drawRoundRect(
                color = defaultColor.copy(alpha = defaultColor.alpha * alpha),
                topLeft = Offset(
                    x = size.width - thicknessPx - paddingPx,
                    y = thumbOffset + paddingPx,
                ),
                size = Size(
                    width = thicknessPx,
                    height = (thumbHeight - (paddingPx * 2)).coerceAtLeast(minThumbLength.toPx() - (paddingPx * 2)),
                ),
                cornerRadius = CornerRadius(thicknessPx / 2, thicknessPx / 2),
            )
        }
    }
}

/**
 * Agrega una barra de desplazamiento (scrollbar) sutil y elegante a un ScrollState vertical estándar.
 */
fun Modifier.kipuScrollbar(
    state: ScrollState,
    color: Color? = null,
    thickness: Dp = 4.dp,
    padding: Dp = 3.dp,
    minThumbLength: Dp = 28.dp,
): Modifier = composed {
    val defaultColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val isScrolling = state.isScrollInProgress

    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isScrolling) 150 else 600,
            delayMillis = if (isScrolling) 0 else 500,
        ),
        label = "scrollStateScrollbarAlpha",
    )

    drawWithContent {
        drawContent()

        val maxValue = state.maxValue
        if (maxValue > 0 && alpha > 0f) {
            val viewportHeight = size.height
            val totalHeight = viewportHeight + maxValue
            val thumbHeightRatio = (viewportHeight / totalHeight).coerceIn(0.12f, 0.9f)
            val thumbHeight = (viewportHeight * thumbHeightRatio).coerceAtLeast(minThumbLength.toPx())

            val scrollProgress = (state.value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            val thumbOffset = scrollProgress * (viewportHeight - thumbHeight)

            val thicknessPx = thickness.toPx()
            val paddingPx = padding.toPx()

            drawRoundRect(
                color = defaultColor.copy(alpha = defaultColor.alpha * alpha),
                topLeft = Offset(
                    x = size.width - thicknessPx - paddingPx,
                    y = thumbOffset + paddingPx,
                ),
                size = Size(
                    width = thicknessPx,
                    height = (thumbHeight - (paddingPx * 2)).coerceAtLeast(minThumbLength.toPx() - (paddingPx * 2)),
                ),
                cornerRadius = CornerRadius(thicknessPx / 2, thicknessPx / 2),
            )
        }
    }
}
