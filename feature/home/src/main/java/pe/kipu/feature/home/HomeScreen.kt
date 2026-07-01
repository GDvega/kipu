package pe.kipu.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAlertCard
import pe.kipu.core.designsystem.component.KipuAlertTone
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuBadge
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuFilterChip
import pe.kipu.core.designsystem.component.KipuHeroCard
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuRegisterFab
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
import pe.kipu.core.designsystem.component.KipuSectionHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuTestTags
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.CycleAvailableBudget
import pe.kipu.core.domain.model.HomePeriodSummary
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.core.domain.util.RelativeDateFormatter
import pe.kipu.feature.home.presentation.HomeAlertTranslator
import pe.kipu.feature.home.presentation.HomeUiState
import pe.kipu.feature.home.presentation.HomeViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign

@Composable
fun HomeScreen(
    onRegisterReceipt: () -> Unit = {},
    onRegisterCash: () -> Unit = {},
    onNavigateToMovements: () -> Unit = {},
    onNavigateToCategoryMovements: (categoryId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                HomeUiState.Loading -> {
                    KipuScreenLoadingState(
                        title = "Tu dinero protegido",
                        greeting = "Hola",
                    )
                }

                is HomeUiState.Content -> {
                    val insights = state.insights
                    if (insights.movementCount == 0 && insights.envelopeCount == 0) {
                        KipuScreenHeader(
                            title = "Tu dinero protegido",
                            greeting = "Hola",
                        )
                        KipuEmptyState(
                            title = "Bienvenido a Kipu",
                            message = "Aún no hay movimientos ni sobres. Empieza registrando tu primer gasto.",
                            actionLabel = "Registrar movimiento",
                            onAction = onRegisterCash,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(KipuLayout.sectionSpacing),
                            contentPadding = PaddingValues(
                                start = KipuLayout.screenHorizontalPadding,
                                end = KipuLayout.screenHorizontalPadding,
                                bottom = 32.dp,
                            ),
                        ) {
                            item {
                                KipuScreenHeader(
                                    title = "Tu dinero protegido",
                                    greeting = "Hola",
                                )
                            }
                            item {
                                DailyAvailableCard(
                                    cycleAvailable = insights.cycleAvailable,
                                    envelopeCount = insights.envelopeCount,
                                    periodSummary = insights.periodSummary,
                                )
                            }
                            insights.cashFlowSummary?.let { summary ->
                                item(key = "cash-flow-summary") {
                                    CashFlowSummaryCard(summary = summary)
                                }
                            }
                            insights.periodSummary?.let { summary ->
                                item(key = "weekly-summary") {
                                    WeeklySummaryCard(summary = summary)
                                }
                            }
                            if (state.userCategories.isNotEmpty()) {
                                item(key = "user-categories-header") {
                                    KipuSectionHeader(title = "Tus categorías")
                                }
                                item(key = "user-categories-chips") {
                                    UserCategoriesRow(
                                        categories = state.userCategories,
                                        onCategoryClick = onNavigateToCategoryMovements,
                                    )
                                }
                            }
                            item {
                                KipuCard {
                                    Text(
                                        text = "¿Pagaste con Yape, Plin o efectivo?",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Comparte un comprobante o registra un gasto en efectivo al instante.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                    KipuPrimaryButton(
                                        text = "Registrar comprobante",
                                        onClick = onRegisterReceipt,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                    )
                                    KipuSecondaryButton(
                                        text = "Registrar en efectivo",
                                        onClick = onRegisterCash,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        fillWidth = true,
                                    )
                                }
                            }
                            if (insights.recentMovements.isNotEmpty()) {
                                item(key = "recent-header") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    ) {
                                        KipuSectionHeader(
                                            title = "Movimientos recientes",
                                            horizontalPadding = 0.dp,
                                        )
                                        KipuTextLink(
                                            text = "Ver todos",
                                            onClick = onNavigateToMovements,
                                        )
                                    }
                                }
                                items(
                                    items = insights.recentMovements,
                                    key = { movement -> "recent-${movement.id}" },
                                ) { movement ->
                                    RecentMovementRow(movement = movement)
                                }
                            }
                            if (insights.antSpendingAlerts.isNotEmpty()) {
                                item {
                                    KipuSectionHeader(title = "Gastos hormiga")
                                }
                                items(
                                    items = insights.antSpendingAlerts,
                                    key = { alert ->
                                        "${alert.categoryId}-${alert.transactionCount}-${alert.totalAmount.amount}"
                                    },
                                ) { alert ->
                                    AntSpendingAlertCard(
                                        alert = alert,
                                        categoryName = state.categoryNamesById[alert.categoryId],
                                    )
                                }
                            }
                        }
                    }
                }

                is HomeUiState.Error -> {
                    KipuErrorState(
                        title = "No pudimos cargar el inicio",
                        message = state.message,
                        retryLabel = "Reintentar",
                        onRetry = viewModel::retryLoad,
                    )
                }
            }
        }
        } // close Scaffold content

        pe.kipu.core.designsystem.component.KipuSpeedDialFab(
            actions = listOf(
                pe.kipu.core.designsystem.component.SpeedDialAction(
                    label = "Registrar ingreso",
                    icon = Icons.Filled.Add,
                    onClick = { /* TODO: Ingreso */ }
                ),
                pe.kipu.core.designsystem.component.SpeedDialAction(
                    label = "Escanear comprobante",
                    icon = Icons.Filled.ShoppingCart,
                    onClick = onRegisterReceipt
                ),
                pe.kipu.core.designsystem.component.SpeedDialAction(
                    label = "Registro manual",
                    icon = Icons.Filled.Edit,
                    onClick = onRegisterCash
                )
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCategoriesRow(
    categories: List<pe.kipu.core.domain.model.Category>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            KipuFilterChip(
                text = category.name,
                selected = false,
                onClick = { onCategoryClick(category.id) },
            )
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    summary: HomePeriodSummary,
    modifier: Modifier = Modifier,
) {
    KipuCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Esta semana",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${formatPenAmountForDisplay(summary.totalCycleSpent.amount)} gastados de " +
                "${formatPenAmountForDisplay(summary.totalCycleLimit.amount)} presupuestados",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        KipuBadge(
            text = "Te quedan ${summary.daysRemainingInCycle} días",
            tone = KipuBadgeTone.Primary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun RecentMovementRow(
    movement: Movement,
    modifier: Modifier = Modifier,
) {
    KipuCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MovementDisplayLabels.displayTitle(
                        movement.counterpartyName,
                        movement.description,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = RelativeDateFormatter.formatDayHeader(movement.recordedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            KipuAmountText(
                amount = movement.amount.amount,
                type = if (movement.type == MovementType.INCOME) {
                    AmountType.INCOME
                } else {
                    AmountType.EXPENSE
                },
            )
        }
    }
}

@Composable
private fun DailyAvailableCard(
    cycleAvailable: CycleAvailableBudget,
    envelopeCount: Int,
    periodSummary: HomePeriodSummary?,
    modifier: Modifier = Modifier,
) {
    val availableAmount = cycleAvailable.cycleAvailable

    // Calculate budget consumption percentage (spent / limit)
    val targetProgress = if (periodSummary != null && periodSummary.totalCycleLimit.amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
        (periodSummary.totalCycleSpent.amount.toFloat() / periodSummary.totalCycleLimit.amount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "BudgetProgress"
    )

    // Determine color mood based on consumption
    val progressColor = when {
        targetProgress >= 0.9f -> MaterialTheme.colorScheme.error // Critical
        targetProgress >= 0.75f -> MaterialTheme.colorScheme.secondary // Warning (Amber)
        else -> MaterialTheme.colorScheme.primary // Safe
    }

    KipuHeroCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(KipuTestTags.DAILY_AVAILABLE_HERO)
            .semantics {
                contentDescription = when {
                    envelopeCount == 0 -> "Configura sobres para ver tu disponible"
                    cycleAvailable.isOverBudget -> "Presupuesto semanal excedido"
                    availableAmount != null ->
                        "Disponible hoy: S/ ${availableAmount.amount.toPlainString()}"
                    else -> "Disponible hoy no calculado"
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DISPONIBLE HOY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                
                when {
                    envelopeCount == 0 -> {
                        Text(
                            text = "Configura sobres para ver tu disponible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    cycleAvailable.isOverBudget -> {
                        KipuBadge(
                            text = "Presupuesto excedido",
                            tone = KipuBadgeTone.Critical,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Text(
                            text = "Ya pasaste tu presupuesto semanal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    cycleAvailable.daysRemainingInCycle <= 0 -> {
                        Text(
                            text = "Sin días restantes esta semana",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    else -> {
                        cycleAvailable.cycleAvailable?.let { amount ->
                            Text(
                                text = formatPenAmountForDisplay(amount.amount),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            KipuBadge(
                                text = "Te quedan ${cycleAvailable.daysRemainingInCycle} días esta semana",
                                tone = KipuBadgeTone.Primary,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
            
            // Dynamic Circular Progress Ring
            if (envelopeCount > 0 && !cycleAvailable.isOverBudget && cycleAvailable.daysRemainingInCycle > 0) {
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            color = progressColor,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }
                    if (periodSummary != null) {
                        Text(
                            text = "${formatPenAmountForDisplay(periodSummary.totalCycleSpent.amount)} gastado de\n${formatPenAmountForDisplay(periodSummary.totalCycleLimit.amount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AntSpendingAlertCard(
    alert: AntSpendingAlert,
    categoryName: String?,
    modifier: Modifier = Modifier,
) {
    val tone = when (alert.severity) {
        AlertSeverity.AMBER -> KipuAlertTone.Warning
        AlertSeverity.RED -> KipuAlertTone.Critical
    }
    val badgeTone = when (alert.severity) {
        AlertSeverity.AMBER -> KipuBadgeTone.Warning
        AlertSeverity.RED -> KipuBadgeTone.Critical
    }
    val titleColor = when (alert.severity) {
        AlertSeverity.AMBER -> MaterialTheme.colorScheme.secondary
        AlertSeverity.RED -> MaterialTheme.colorScheme.error
    }

    KipuAlertCard(tone = tone, modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Gasto hormiga detectado",
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor,
        )
        Text(
            text = HomeAlertTranslator.toDisplayText(alert, categoryName),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        KipuBadge(
            text = "${alert.transactionCount} movimientos en 48 h",
            tone = badgeTone,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun CashFlowSummaryCard(
    summary: pe.kipu.core.domain.model.CashFlowSummary,
    modifier: Modifier = Modifier,
) {
    KipuCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Efectivo real",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Total ingresado menos total gastado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        KipuAmountText(
            amount = summary.netCash.amount,
            type = AmountType.INCOME,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (summary.isGoalAtRisk) {
            KipuBadge(
                text = "Metas en riesgo",
                tone = KipuBadgeTone.Critical,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "El efectivo real es menor que la suma de tus metas de ahorro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
