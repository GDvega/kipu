package pe.kipu.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuCardStyle
import pe.kipu.core.designsystem.component.KipuCompactBadge
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.feature.movements.presentation.channelLabel
import pe.kipu.feature.movements.presentation.channelVisual
import pe.kipu.feature.movements.presentation.confidenceIsLow
import pe.kipu.feature.movements.presentation.confidenceLabel
import pe.kipu.feature.movements.presentation.movementDisplayTitle
import pe.kipu.feature.movements.presentation.sourceBadgeTone
import pe.kipu.feature.movements.presentation.sourceLabel
import pe.kipu.feature.movements.presentation.statusBadgeTone
import pe.kipu.feature.movements.presentation.statusLabel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background

@Composable
fun MovementHtmlCard(
    movement: Movement,
    categoryName: String?,
    onChangeCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = movement.channelVisual()

    KipuCard(modifier = modifier, style = KipuCardStyle.Large) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(visual.iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = movement.channelIcon(),
                    contentDescription = null,
                    tint = visual.iconTint,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movementDisplayTitle(movement.counterpartyName, movement.description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatPenAmountForDisplay(movement.amount.amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    MetaDot(modifier = Modifier.padding(horizontal = 4.dp))
                    KipuCompactBadge(
                        text = movement.sourceLabel(),
                        tone = movement.sourceBadgeTone(),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = movement.channelLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    if (categoryName != null) {
                        MetaDot(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    MetaDot(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = movement.confidenceLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (movement.confidenceIsLow()) KipuAmber else KipuPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                KipuCompactBadge(
                    text = movement.statusLabel(),
                    tone = movement.statusBadgeTone(),
                    modifier = Modifier.padding(top = 8.dp),
                )
                KipuTextLink(
                    text = "Cambiar categoría",
                    onClick = onChangeCategory,
                )
            }
        }
    }
}

@Composable
private fun MetaDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(3.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.onSurfaceVariant),
    )
}

private fun Movement.channelIcon(): ImageVector = when (channel) {
    PaymentChannel.YAPE -> Icons.Filled.Share
    PaymentChannel.PLIN -> Icons.AutoMirrored.Filled.Send
    PaymentChannel.CASH -> Icons.Filled.Star
    PaymentChannel.MANUAL,
    PaymentChannel.OTHER,
    -> Icons.Filled.Star
}
