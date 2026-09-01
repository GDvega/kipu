package pe.kipu.feature.movements.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuBadge
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.feature.movements.presentation.actionBadgeTone
import pe.kipu.feature.movements.presentation.actionLabel
import pe.kipu.feature.movements.presentation.channelVisual
import pe.kipu.feature.movements.presentation.formatMovementDateTime
import pe.kipu.feature.movements.presentation.movementDisplayTitle

@Composable
fun MovementAuditCard(
    audit: MovementAuditEntry,
    modifier: Modifier = Modifier,
) {
    val isDeleted = audit.action == MovementAuditAction.DELETED
    val isIncome = audit.movementType == MovementType.INCOME
    val visual = audit.channelVisual()

    KipuCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Channel icon with background circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(visual.iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (audit.action) {
                            MovementAuditAction.DELETED -> Icons.Default.Delete
                            MovementAuditAction.UPDATED -> Icons.Default.Edit
                            MovementAuditAction.CREATED -> when (audit.channel) {
                                PaymentChannel.YAPE, PaymentChannel.PLIN -> Icons.AutoMirrored.Filled.Send
                                else -> Icons.Default.Star
                            }
                        },
                        contentDescription = audit.channel.name,
                        tint = visual.iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Date
                Column(modifier = Modifier.weight(1f)) {
                    val title = movementDisplayTitle(audit.counterpartyName, audit.description)
                        .ifBlank { audit.categoryName ?: "Movimiento" }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formatMovementDateTime(audit.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount
                val formattedAmount = formatPenAmountForDisplay(audit.amount.amount, showSign = isIncome)

                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isDeleted -> MaterialTheme.colorScheme.onSurfaceVariant
                        isIncome -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Action badge, Category, and Channel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KipuBadge(
                    text = audit.actionLabel(),
                    tone = audit.actionBadgeTone(),
                )

                audit.categoryName?.let { catName ->
                    KipuBadge(
                        text = catName,
                        tone = KipuBadgeTone.Info,
                    )
                }

                KipuBadge(
                    text = when (audit.channel) {
                        PaymentChannel.YAPE -> "Yape"
                        PaymentChannel.PLIN -> "Plin"
                        PaymentChannel.CASH -> "Efectivo"
                        PaymentChannel.MANUAL -> "Banco"
                        PaymentChannel.OTHER -> "Otro"
                    },
                    tone = KipuBadgeTone.Info,
                )
            }

            // Details / Diff note
            val detailsText = audit.details
            if (!detailsText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = detailsText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
