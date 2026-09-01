package pe.kipu.feature.envelopes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuCardStyle
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.kipuScrollbar
import pe.kipu.core.designsystem.component.KipuPlanShortcut
import pe.kipu.core.designsystem.component.KipuPlanShortcutRow
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLinearProgress
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Movement
import pe.kipu.feature.envelopes.presentation.EnvelopeBudgetUiModel
import pe.kipu.feature.envelopes.presentation.EnvelopesUiState
import pe.kipu.feature.envelopes.presentation.EnvelopesViewModel
import pe.kipu.feature.envelopes.presentation.daysRemainingLabel
import pe.kipu.feature.envelopes.presentation.percentLabel
import pe.kipu.feature.envelopes.presentation.visualStyle
import pe.kipu.feature.envelopes.ui.EnvelopeAdjustLimitDialog
import pe.kipu.feature.envelopes.ui.EnvelopeCreateDialog
import pe.kipu.feature.envelopes.ui.EnvelopeDeleteConfirmDialog
import pe.kipu.feature.envelopes.ui.EnvelopePlanBalanceBanner
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.core.designsystem.component.KipuCard

@Composable
fun EnvelopesScreen(
    onNavigateToMovements: (String) -> Unit,
    onNavigateToPlan: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnvelopesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            EnvelopesUiState.Loading -> {
                KipuScreenLoadingState(
                    title = "Mis Sobres",
                    subtitle = "Tus presupuestos semanales y metas de ahorro",
                )
            }

            is EnvelopesUiState.Content -> {
                state.adjustTarget?.let { budget ->
                    EnvelopeAdjustLimitDialog(
                        budget = budget,
                        errorMessage = state.adjustLimitError,
                        isSaving = state.isAdjustingLimit,
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
                        isDeleting = state.isDeleting,
                        errorMessage = state.deleteErrorMessage,
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
                    val envelopesListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LazyColumn(
                        state = envelopesListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .kipuScrollbar(envelopesListState),
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
                            EnvelopePlanShortcuts(
                                onNavigateToPlan = onNavigateToPlan,
                                modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                            )
                        }
                        state.planBalance?.let { balance ->
                            item(key = "plan-balance") {
                                EnvelopePlanBalanceBanner(
                                    summary = balance,
                                    modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                                )
                            }
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
                        itemsIndexed(
                            items = state.budgets,
                            key = { _, item -> item.budget.envelopeId },
                        ) { index, item ->
                            pe.kipu.core.designsystem.component.KipuAnimatedListItem(index = index) {
                                EnvelopeDetailCard(
                                    item = item,
                                    budgetCycle = state.budgetCycle,
                                    onViewMovements = { onNavigateToMovements(item.budget.categoryId) },
                                    onAdjust = { viewModel.onAdjustClick(item.budget) },
                                    onDelete = { viewModel.onDeleteClick(item.budget) },
                                    modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                                )
                            }
                        }
                    }
                }
            }

            is EnvelopesUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar los sobres",
                    message = state.message,
                    retryLabel = "Reintentar",
                    onRetry = viewModel::retryLoad,
                )
            }
        }
    }
}

@Composable
private fun EnvelopePlanShortcuts(
    onNavigateToPlan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    KipuPlanShortcutRow(
        shortcuts = listOf(
            KipuPlanShortcut(
                label = "Ingresos",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = pe.kipu.core.designsystem.theme.KipuPrimary,
            ) { onNavigateToPlan("income") },
            KipuPlanShortcut(
                label = "Gastos",
                icon = Icons.Filled.Receipt,
                iconTint = pe.kipu.core.designsystem.theme.KipuAmber,
            ) { onNavigateToPlan("expenses") },
            KipuPlanShortcut(
                label = "Sobres",
                icon = Icons.Filled.AccountBalanceWallet,
                iconTint = pe.kipu.core.designsystem.theme.KipuBlue,
            ) { onNavigateToPlan("envelopes") },
            KipuPlanShortcut(
                label = "Meta",
                icon = Icons.Filled.Savings,
                iconTint = pe.kipu.core.designsystem.theme.KipuPurple,
            ) { onNavigateToPlan("goal") },
        ),
        modifier = modifier,
    )
}

@Composable
private fun EnvelopeDetailCard(
    item: EnvelopeBudgetUiModel,
    budgetCycle: pe.kipu.core.domain.model.BudgetCycle,
    onViewMovements: () -> Unit,
    onAdjust: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val budget = item.budget
    val style = budget.visualStyle()
    val progress = (budget.percentUsed.coerceAtLeast(0).toFloat() / 100f).coerceAtMost(1f)
    val percentToneColor = when {
        budget.status == EnvelopeBudgetStatus.EXCEEDED || budget.percentUsed >= 100 ->
            MaterialTheme.colorScheme.error
        budget.status == EnvelopeBudgetStatus.ADJUSTED || budget.percentUsed >= 75 ->
            MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val cycleSuffix = when (budgetCycle) {
        pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> "semanal"
        pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> "mensual"
        pe.kipu.core.domain.model.BudgetCycle.DAILY -> "diario"
    }

    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${budget.name}, ${budget.percentLabel()} usado, " +
                    "te quedan ${formatPenAmountForDisplay(budget.remainingAmount.amount)}"
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(style.progressColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
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
                                "${formatPenAmountForDisplay(budget.weeklyLimit.amount)} $cycleSuffix",
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

                Text(
                    text = "Te quedan ${formatPenAmountForDisplay(budget.remainingAmount.amount)} · " +
                        "${budget.percentLabel()} usado · ${pe.kipu.feature.envelopes.presentation.daysRemainingLabel(budgetCycle)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = percentToneColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )

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
                    destructive = true,
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
                        text = "Sin movimientos confirmados este ciclo.",
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

private fun EnvelopeBudgetState.categoryIcon(): ImageVector = when (categoryId) {
    CategoryIds.FOOD -> Icons.Filled.ShoppingCart
    CategoryIds.TRANSPORT -> Icons.Filled.Place
    CategoryIds.SERVICES -> Icons.Filled.Home
    else -> when {
        name.contains("hormiga", ignoreCase = true) -> Icons.Filled.ShoppingCart
        name.contains("meta", ignoreCase = true) -> Icons.Filled.Savings
        else -> Icons.Filled.AccountBalanceWallet
    }
}
