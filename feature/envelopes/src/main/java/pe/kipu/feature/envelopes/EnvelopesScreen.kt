package pe.kipu.feature.envelopes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuCardStyle
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuFilterChip
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLinearProgress
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Movement
import pe.kipu.feature.envelopes.presentation.EnvelopeBudgetUiModel
import pe.kipu.feature.envelopes.presentation.EnvelopesUiState
import pe.kipu.feature.envelopes.presentation.EnvelopesViewModel
import pe.kipu.feature.envelopes.presentation.daysRemainingLabel
import pe.kipu.feature.envelopes.presentation.percentLabel
import pe.kipu.feature.envelopes.presentation.percentToneColor
import pe.kipu.feature.envelopes.presentation.visualStyle
import pe.kipu.feature.envelopes.ui.EnvelopeAdjustLimitDialog
import pe.kipu.feature.envelopes.ui.EnvelopeCreateDialog
import pe.kipu.feature.envelopes.ui.EnvelopeDeleteConfirmDialog
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.core.designsystem.component.KipuCard

@Composable
fun EnvelopesScreen(
    onNavigateToMovements: (String) -> Unit,
    onNavigateToPlan: (String) -> Unit,
    onNavigateToCommitments: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnvelopesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            EnvelopesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    KipuLoadingIndicator()
                }
            }

            is EnvelopesUiState.Content -> {
                state.adjustTarget?.let { budget ->
                    EnvelopeAdjustLimitDialog(
                        budget = budget,
                        onSave = viewModel::onSaveWeeklyLimit,
                        onDismiss = viewModel::onDismissAdjust,
                    )
                }
                if (state.showCreateDialog) {
                    val available = state.categories.filter { it.id !in state.usedCategoryIds }
                    EnvelopeCreateDialog(
                        availableCategories = available,
                        formState = state.createForm,
                        onNameChanged = viewModel::onCreateNameChanged,
                        onCategorySelected = viewModel::onCreateCategorySelected,
                        onAmountChanged = viewModel::onCreateAmountChanged,
                        onConfirm = viewModel::onConfirmCreate,
                        onDismiss = viewModel::onDismissCreate,
                    )
                }
                state.deleteTarget?.let { target ->
                    EnvelopeDeleteConfirmDialog(
                        envelopeName = target.name,
                        onConfirm = viewModel::onConfirmDelete,
                        onDismiss = viewModel::onDismissDelete,
                    )
                }

                val availableCategories = state.categories.filter { it.id !in state.usedCategoryIds }

                if (state.budgets.isEmpty()) {
                    KipuScreenHeader(
                        title = "Mis Sobres",
                        subtitle = "Tus presupuestos semanales y metas de ahorro",
                    )
                    KipuEmptyState(
                        title = "Sin sobres",
                        message = if (availableCategories.isNotEmpty()) {
                            "Crea tu primer sobre para organizar tu presupuesto semanal."
                        } else {
                            "Configura tu plan financiero para crear sobres semanales."
                        },
                        actionLabel = if (availableCategories.isNotEmpty()) "Nuevo sobre" else "Configurar plan",
                        onAction = {
                            if (availableCategories.isNotEmpty()) {
                                viewModel.onCreateClick()
                            } else {
                                onNavigateToPlan("expenses")
                            }
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KipuLayout.sectionSpacing),
                        contentPadding = PaddingValues(bottom = KipuLayout.screenHorizontalPadding),
                    ) {
                        item {
                            KipuScreenHeader(
                                title = "Mis Sobres",
                                subtitle = "Tus presupuestos semanales y metas de ahorro",
                            )
                        }
                        item {
                            EnvelopeActionsRow(
                                onNavigateToPlan = onNavigateToPlan,
                                onNavigateToCommitments = onNavigateToCommitments,
                                modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                            )
                        }
                        if (availableCategories.isNotEmpty()) {
                            item(key = "new-envelope") {
                                KipuSecondaryButton(
                                    text = "Nuevo sobre",
                                    onClick = viewModel::onCreateClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = KipuLayout.screenHorizontalPadding),
                                    fillWidth = true,
                                )
                            }
                        }
                        items(
                            items = state.budgets,
                            key = { item -> item.budget.envelopeId },
                        ) { item ->
                            EnvelopeDetailCard(
                                item = item,
                                onViewMovements = { onNavigateToMovements(item.budget.categoryId) },
                                onAdjust = { viewModel.onAdjustClick(item.budget) },
                                onDelete = { viewModel.onDeleteClick(item.budget) },
                                modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                            )
                        }
                    }
                }
            }

            is EnvelopesUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar los sobres",
                    message = state.message,
                )
            }
        }
    }
}

@Composable
private fun EnvelopeActionsRow(
    onNavigateToPlan: (String) -> Unit,
    onNavigateToCommitments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KipuFilterChip(
            text = "Ingresos",
            selected = false,
            onClick = { onNavigateToPlan("income") },
            modifier = Modifier.weight(1f),
        )
        KipuFilterChip(
            text = "Gastos",
            selected = false,
            onClick = { onNavigateToPlan("expenses") },
            modifier = Modifier.weight(1f),
        )
        KipuFilterChip(
            text = "Meta",
            selected = false,
            onClick = onNavigateToCommitments,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EnvelopeDetailCard(
    item: EnvelopeBudgetUiModel,
    onViewMovements: () -> Unit,
    onAdjust: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val budget = item.budget
    val style = budget.visualStyle()
    val progress = (budget.percentUsed.coerceAtLeast(0).toFloat() / 100f).coerceAtMost(1f)

    KipuCard(modifier = modifier, style = KipuCardStyle.Large) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(style.iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = budget.categoryIcon(),
                    contentDescription = null,
                    tint = style.iconTint,
                )
            }
            Column {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "${formatPenAmountForDisplay(budget.spentAmount.amount)} usado de " +
                        "${formatPenAmountForDisplay(budget.weeklyLimit.amount)} semanal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        KipuLinearProgress(
            progress = progress,
            fillColor = style.progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EnvelopeStatCell(
                value = formatPenAmountForDisplay(budget.remainingAmount.amount),
                label = "Restante",
                modifier = Modifier.weight(1f),
            )
            EnvelopeStatCell(
                value = budget.percentLabel(),
                label = "Usado",
                modifier = Modifier.weight(1f),
                valueColor = budget.percentToneColor(),
            )
            EnvelopeStatCell(
                value = daysRemainingLabel(),
                label = "Restan",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KipuPrimaryButton(
                text = "Ver movimientos",
                onClick = onViewMovements,
                modifier = Modifier.weight(1f),
                fillWidth = true,
            )
            KipuSecondaryButton(
                text = "Ajustar",
                onClick = onAdjust,
                modifier = Modifier.weight(1f),
                fillWidth = true,
            )
        }
        KipuSecondaryButton(
            text = "Eliminar sobre",
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            fillWidth = true,
        )

        Text(
            text = "Últimos movimientos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        if (item.recentMovements.isEmpty()) {
            Text(
                text = "Sin movimientos confirmados esta semana.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            item.recentMovements.forEach { movement ->
                EnvelopeRecentMovementRow(movement = movement)
            }
        }
    }
}

@Composable
private fun EnvelopeRecentMovementRow(movement: Movement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = MovementDisplayLabels.displayTitle(movement.counterpartyName, movement.description),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = MovementDisplayLabels.formatDateTime(movement.recordedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = formatPenAmountForDisplay(movement.amount.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EnvelopeStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val resolvedColor = valueColor ?: MaterialTheme.colorScheme.onSurface
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = resolvedColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun EnvelopeBudgetState.categoryIcon(): ImageVector = when (categoryId) {
    CategoryIds.FOOD -> Icons.Filled.Star
    CategoryIds.TRANSPORT -> Icons.Filled.Share
    CategoryIds.SERVICES -> Icons.Filled.Home
    else -> Icons.Filled.Star
}
