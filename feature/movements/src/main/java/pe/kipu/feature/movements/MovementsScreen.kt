package pe.kipu.feature.movements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuRegisterFab
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSectionHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.feature.movements.presentation.DuplicateResolutionDialog
import pe.kipu.feature.movements.presentation.MovementChannelFilter
import pe.kipu.feature.movements.presentation.MovementDuplicateTranslator
import pe.kipu.feature.movements.presentation.MovementsUiState
import pe.kipu.feature.movements.presentation.MovementsViewModel
import pe.kipu.feature.movements.presentation.PendingNotificationDuplicateDialog
import pe.kipu.feature.movements.presentation.PendingNotificationIncomeCard
import pe.kipu.feature.movements.presentation.groupMovementsByDay
import pe.kipu.feature.movements.presentation.movementDisplayTitle
import pe.kipu.feature.movements.ui.CategoryChangeDialog
import pe.kipu.feature.movements.ui.GoalLinkDialog
import pe.kipu.feature.movements.ui.ManualMovementDialog
import pe.kipu.feature.movements.ui.MovementHtmlCard

@Composable
fun MovementsScreen(
    initialCategoryId: String? = null,
    onRegisterReceipt: () -> Unit = {},
    openManualOnLaunch: Boolean = false,
    onOpenManualLaunchConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is pe.kipu.feature.movements.presentation.MovementsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    LaunchedEffect(initialCategoryId) {
        viewModel.applyCategoryFilter(initialCategoryId)
    }

    LaunchedEffect(openManualOnLaunch, uiState) {
        if (openManualOnLaunch && uiState is MovementsUiState.Content) {
            viewModel.onRegisterManualClicked(pe.kipu.core.domain.model.PaymentChannel.CASH)
            onOpenManualLaunchConsumed()
        }
    }

    val showRegisterFab = (uiState as? MovementsUiState.Content)?.let { state ->
        state.filteredMovements.isNotEmpty() ||
            state.duplicatePairs.isNotEmpty() ||
            state.pendingNotificationIncomes.isNotEmpty()
    } == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showRegisterFab) {
                KipuRegisterFab(onClick = viewModel::onAddMovementClick)
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                MovementsUiState.Loading -> {
                    KipuScreenLoadingState(
                        title = "Movimientos",
                        subtitle = "Yape, Plin, efectivo y más",
                    )
                }

                is MovementsUiState.Content -> {
                state.pendingResolution?.let { pair ->
                    DuplicateResolutionDialog(
                        pair = pair,
                        onResolve = viewModel::onResolveDuplicate,
                        isProcessing = state.isActionInProgress,
                    )
                }

                state.pendingNotificationConfirm?.let { confirmState ->
                    val pendingMovement = state.pendingNotificationIncomes.find {
                        it.id == confirmState.movementId
                    }
                    val existingMatch = confirmState.duplicateMatches.firstOrNull()
                    LaunchedEffect(confirmState, pendingMovement?.id, existingMatch?.id) {
                        if (pendingMovement == null || existingMatch == null) {
                            viewModel.clearStalePendingNotificationConfirm()
                        }
                    }
                    if (pendingMovement != null && existingMatch != null) {
                        PendingNotificationDuplicateDialog(
                            pendingMovement = pendingMovement,
                            existingMatch = existingMatch,
                            onResolve = viewModel::onResolvePendingNotificationDuplicate,
                            isProcessing = state.isActionInProgress,
                        )
                    }
                }

                state.categoryChangeTarget?.let { movement ->
                    CategoryChangeDialog(
                        movement = movement,
                        categories = state.categories,
                        currentCategoryName = state.categoryNamesById[movement.categoryId],
                        isProcessing = state.isActionInProgress,
                        onCategorySelected = viewModel::onCategorySelected,
                        onDismiss = viewModel::onDismissCategoryChange,
                    )
                }

                state.goalLinkTarget?.let { movement ->
                    GoalLinkDialog(
                        movement = movement,
                        savingsGoals = state.savingsGoals,
                        currentGoalTitle = movement.commitmentId?.let { id ->
                            state.savingsGoals.find { it.id == id }?.title
                        },
                        isProcessing = state.isActionInProgress,
                        onGoalSelected = viewModel::onGoalSelected,
                        onDismiss = viewModel::onDismissGoalLink,
                    )
                }

                state.manualMovementForm?.let { form ->
                    ManualMovementDialog(
                        categories = state.categories,
                        formState = form,
                        onMovementTypeSelected = viewModel::onManualMovementTypeSelected,
                        onChannelSelected = viewModel::onManualChannelSelected,
                        onAmountChanged = viewModel::onManualAmountChanged,
                        onCategorySelected = viewModel::onManualCategorySelected,
                        onDescriptionChanged = viewModel::onManualDescriptionChanged,
                        onCounterpartyChanged = viewModel::onManualCounterpartyChanged,
                        onConfirm = viewModel::onSaveManualMovement,
                        onDismiss = viewModel::onDismissManualMovement,
                    )
                }

                KipuScreenHeader(
                    title = "Movimientos",
                    subtitle = "Yape, Plin, efectivo y más",
                )

                val filters = MovementChannelFilter.entries
                KipuFilterChipRow(
                    labels = filters.map { it.label },
                    selectedIndex = filters.indexOf(state.selectedFilter),
                    onSelected = { index -> viewModel.onFilterSelected(filters[index]) },
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                if (state.categoryFilterName != null) {
                    RowCategoryFilterBanner(
                        categoryName = state.categoryFilterName,
                        onClear = viewModel::clearCategoryFilter,
                        modifier = Modifier.padding(
                            horizontal = KipuLayout.screenHorizontalPadding,
                            vertical = 8.dp,
                        ),
                    )
                }

                val hasContent = showRegisterFab

                if (!hasContent) {
                    KipuEmptyState(
                        title = "Sin movimientos",
                        message = if (state.selectedFilter == MovementChannelFilter.ALL) {
                            "Registra tu primer gasto o ingreso para empezar."
                        } else {
                            "No hay movimientos con el filtro ${state.selectedFilter.label}."
                        },
                        actionLabel = "Registrar movimiento",
                        onAction = viewModel::onAddMovementClick,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KipuLayout.listItemSpacing),
                        contentPadding = KipuLayout.listContentPadding(fabClearance = true),
                    ) {
                        if (state.pendingNotificationIncomes.isNotEmpty()) {
                            item(key = "pending-notifications-header") {
                                KipuSectionHeader(
                                    title = "Ingresos por confirmar",
                                    horizontalPadding = 0.dp,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            itemsIndexed(
                                items = state.pendingNotificationIncomes,
                                key = { _, movement -> "pending-${movement.id}" },
                            ) { index, movement ->
                                pe.kipu.core.designsystem.component.KipuAnimatedListItem(index = index) {
                                    KipuCard {
                                        PendingNotificationIncomeCard(
                                            movement = movement,
                                            onConfirm = { viewModel.onConfirmPendingNotification(movement.id) },
                                            onDismiss = { viewModel.onDismissPendingNotification(movement.id) },
                                            enabled = !state.isActionInProgress,
                                        )
                                    }
                                }
                            }
                        }
                        if (state.duplicatePairs.isNotEmpty()) {
                            item(key = "duplicates-header") {
                                KipuSectionHeader(
                                    title = "Posibles duplicados",
                                    horizontalPadding = 0.dp,
                                    modifier = Modifier.padding(
                                        top = if (state.pendingNotificationIncomes.isNotEmpty()) {
                                            KipuLayout.sectionSpacing
                                        } else {
                                            0.dp
                                        },
                                        bottom = 4.dp,
                                    ),
                                )
                            }
                            itemsIndexed(
                                items = state.duplicatePairs,
                                key = { _, pair -> "dup-${pair.movementA.id}-${pair.movementB.id}" },
                            ) { index, pair ->
                                pe.kipu.core.designsystem.component.KipuAnimatedListItem(index = index) {
                                    DuplicatePairListItem(
                                        pair = pair,
                                        onClick = { viewModel.onDuplicatePairClick(pair) },
                                    )
                                }
                            }
                        }
                        if (state.filteredMovements.isNotEmpty()) {
                            val dayGroups = groupMovementsByDay(state.filteredMovements)
                            dayGroups.forEach { group ->
                                item(key = "day-${group.dayKey}") {
                                    KipuSectionHeader(
                                        title = group.headerLabel,
                                        horizontalPadding = 0.dp,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                                itemsIndexed(
                                    items = group.movements,
                                    key = { _, movement -> movement.id },
                                ) { index, movement ->
                                    pe.kipu.core.designsystem.component.KipuAnimatedListItem(index = index) {
                                        val linkedGoalTitle = movement.commitmentId?.let { goalId ->
                                            state.savingsGoals.find { it.id == goalId }?.title
                                        }
                                        MovementHtmlCard(
                                            movement = movement,
                                            categoryName = state.categoryNamesById[movement.categoryId],
                                            linkedGoalTitle = linkedGoalTitle,
                                            onChangeCategory = { viewModel.onChangeCategoryClick(movement) },
                                            onLinkGoal = { viewModel.onLinkGoalClick(movement) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is MovementsUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar los movimientos",
                    message = state.message,
                    retryLabel = "Reintentar",
                    onRetry = viewModel::retryLoad,
                )
            }
        }
        }
    }
}

@Composable
private fun RowCategoryFilterBanner(
    categoryName: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Sobre: $categoryName",
            style = MaterialTheme.typography.labelLarge,
        )
        KipuTextLink(text = "Ver todos", onClick = onClear)
    }
}

@Composable
private fun DuplicatePairListItem(
    pair: MovementDuplicatePair,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstTitle = movementDisplayTitle(pair.movementA.counterpartyName, pair.movementA.description)
    val secondTitle = movementDisplayTitle(pair.movementB.counterpartyName, pair.movementB.description)
    val reasonText = MovementDuplicateTranslator.matchReasonText(pair.matchReasonKey)

    KipuCard(
        modifier = modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        Column {
            Text(
                text = reasonText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "$firstTitle y $secondTitle",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Toca para resolver",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
