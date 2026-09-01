package pe.kipu.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuCardStyle
import pe.kipu.core.designsystem.component.KipuCompactBadge
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.feature.movements.presentation.channelLabel
import pe.kipu.feature.movements.presentation.channelVisual
import pe.kipu.feature.movements.presentation.confidenceIsLow
import pe.kipu.feature.movements.presentation.confidenceLabel
import pe.kipu.feature.movements.presentation.movementDisplayTitle
import pe.kipu.feature.movements.presentation.sourceBadgeTone
import pe.kipu.feature.movements.presentation.sourceLabel
import pe.kipu.feature.movements.presentation.statusBadgeTone
import pe.kipu.feature.movements.presentation.statusLabel
import androidx.compose.foundation.background

@Composable
fun MovementHtmlCard(
    movement: Movement,
    categoryName: String?,
    linkedGoalTitle: String? = null,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onChangeCategory: () -> Unit = {},
    onLinkGoal: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val visual = movement.channelVisual()
    val title = movementDisplayTitle(movement.counterpartyName, movement.description)
    val amountType = if (movement.type == MovementType.INCOME) AmountType.INCOME else AmountType.EXPENSE
    val dateLabel = MovementDisplayLabels.formatDateTime(movement.recordedAt)
    val accessibilityLabel = buildString {
        append(if (movement.type == MovementType.INCOME) "Ingreso" else "Gasto")
        append(" ${movement.channelLabel()}, ")
        append(formatPenAmountForDisplay(movement.amount.amount))
        append(", $title")
        categoryName?.let { append(", $it") }
        append(", $dateLabel")
        if (movement.status == MovementStatus.PENDING_CONFIRMATION) {
            append(", pendiente de confirmación")
        }
    }

    KipuCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = accessibilityLabel
        },
        style = KipuCardStyle.Large,
    ) {
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
                    contentDescription = movement.channelLabel(),
                    tint = visual.iconTint,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    KipuAmountText(
                        amount = movement.amount.amount,
                        type = amountType,
                    )
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KipuCompactBadge(
                        text = movement.sourceLabel(),
                        tone = movement.sourceBadgeTone(),
                    )
                    MetaDot(modifier = Modifier.padding(horizontal = 4.dp))
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
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KipuCompactBadge(
                        text = movement.statusLabel(),
                        tone = movement.statusBadgeTone(),
                    )
                    if (movement.confidenceIsLow()) {
                        KipuCompactBadge(
                            text = movement.confidenceLabel(),
                            tone = pe.kipu.core.designsystem.component.KipuBadgeTone.Warning,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KipuTextLink(
                        text = "Editar",
                        onClick = onEdit,
                    )
                    KipuTextLink(
                        text = "Eliminar",
                        onClick = onDelete,
                    )
                    KipuTextLink(
                        text = "Categoría",
                        onClick = onChangeCategory,
                    )
                }
                if (movement.type == MovementType.INCOME && onLinkGoal != null) {
                    if (linkedGoalTitle != null) {
                        Text(
                            text = "Meta: $linkedGoalTitle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    KipuTextLink(
                        text = if (linkedGoalTitle == null) "Vincular a meta" else "Cambiar meta",
                        onClick = onLinkGoal,
                    )
                }
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
