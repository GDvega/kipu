package pe.kipu.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuSegment
import pe.kipu.core.designsystem.component.KipuSegmentedBar
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.designsystem.theme.getChartColor
import pe.kipu.core.domain.model.CategoryExpenseDistribution
import pe.kipu.core.domain.model.CategoryExpenseSlice

@Composable
fun CategoryDistributionCard(
    distribution: CategoryExpenseDistribution,
    onCategoryClick: (categoryId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (distribution.isEmpty) return

    var isExpanded by rememberSaveable { mutableStateOf(true) }

    val segments = distribution.slices.map { slice ->
        KipuSegment(
            percentage = slice.percentage,
            color = getChartColor(slice.colorIndex),
            label = slice.categoryName,
        )
    }

    KipuCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            // Header: Title, Subtitle and Total Spent (Clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        role = Role.Button,
                        onClick = { isExpanded = !isExpanded },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Distribución de gastos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val topCat = distribution.topCategory
                    val subtitle = if (topCat != null) {
                        "Mayor gasto en ${topCat.categoryName} (${topCat.percentageFormatted})"
                    } else {
                        "Desglose por categorías en el ciclo"
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatPenAmountForDisplay(distribution.totalSpent.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            text = "${distribution.totalTransactions} movs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Plegar desglose" else "Desplegar desglose",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isExpanded) 180f else 0f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Graphical Segmented Bar
            KipuSegmentedBar(
                segments = segments,
                height = 10.dp,
                gap = 2.dp,
                cornerRadius = 5.dp,
            )

            // Slices Breakdown List
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    distribution.slices.forEach { slice ->
                        CategoryDistributionRow(
                            slice = slice,
                            onClick = { onCategoryClick(slice.categoryId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionRow(
    slice: CategoryExpenseSlice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = getChartColor(slice.colorIndex)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Category Name and transaction count
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slice.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "${slice.transactionCount} ${if (slice.transactionCount == 1) "movimiento" else "movimientos"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Percentage Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                text = slice.percentageFormatted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Total Amount
        Text(
            text = formatPenAmountForDisplay(slice.totalAmount.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp),
        )
    }
}
