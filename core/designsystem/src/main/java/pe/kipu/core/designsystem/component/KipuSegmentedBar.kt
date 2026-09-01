package pe.kipu.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single data segment for [KipuSegmentedBar].
 */
data class KipuSegment(
    val percentage: Float,
    val color: Color,
    val label: String? = null,
)

/**
 * Multi-colored proportional segmented bar chart with rounded edges and animations.
 */
@Composable
fun KipuSegmentedBar(
    segments: List<KipuSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    gap: Dp = 2.dp,
    cornerRadius: Dp = 6.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
) {
    val validSegments = segments.filter { it.percentage > 0.0001f }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
    ) {
        if (validSegments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                validSegments.forEachIndexed { index, segment ->
                    if (index > 0 && gap > 0.dp) {
                        Spacer(modifier = Modifier.width(gap))
                    }

                    val animatedWeight by animateFloatAsState(
                        targetValue = segment.percentage.coerceIn(0.001f, 1.0f),
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "segment_weight_$index",
                    )

                    Box(
                        modifier = Modifier
                            .weight(animatedWeight)
                            .fillMaxHeight()
                            .background(segment.color),
                    )
                }
            }
        }
    }
}
