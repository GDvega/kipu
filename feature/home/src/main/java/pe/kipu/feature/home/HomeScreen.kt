package pe.kipu.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import pe.kipu.core.designsystem.component.KipuHeroCard
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuRegisterFab
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuSectionHeader
import pe.kipu.core.designsystem.component.KipuTestTags
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.designsystem.component.KipuVoiceFab
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.designsystem.component.kipuScrollbar
import pe.kipu.core.designsystem.voice.VoiceSpeechManager
import pe.kipu.core.designsystem.voice.VoiceSpeechState
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AvailableBalance
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.CashFlowSummary
import pe.kipu.core.domain.model.CycleAvailableBudget
import pe.kipu.core.domain.model.HomePeriodSummary
import pe.kipu.core.domain.model.MonthlyBudgetSummary
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveBalance
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.core.domain.util.RelativeDateFormatter
import pe.kipu.core.domain.voice.VoiceFinancialIntent
import pe.kipu.feature.home.presentation.HomeAlertTranslator
import pe.kipu.feature.home.presentation.HomeCycleText
import pe.kipu.feature.home.presentation.HomeUiState
import pe.kipu.feature.home.presentation.HomeViewModel
import pe.kipu.feature.home.ui.CategoryDistributionCard
import pe.kipu.feature.home.ui.MonthlyReceiptsCard
import pe.kipu.feature.home.ui.VoiceConfirmationBottomSheet

@Composable
fun HomeScreen(
    onRegisterReceipt: () -> Unit = {},
    onRegisterCash: () -> Unit = {},
    onNavigateToMovements: () -> Unit = {},
    onNavigateToCategoryMovements: (categoryId: String) -> Unit = {},
    onNavigateToPlan: (startStep: String) -> Unit = {},
    speedDialModalBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val parsedIntent by viewModel.parsedVoiceIntent.collectAsStateWithLifecycle()
    val isAnalyzingVoice by viewModel.isAnalyzingVoice.collectAsStateWithLifecycle()
    val isSavingVoice by viewModel.isSavingVoice.collectAsStateWithLifecycle()
    val voiceSaveError by viewModel.voiceSaveError.collectAsStateWithLifecycle()
    val voiceUnexpectedExpense by viewModel.voiceUnexpectedExpense.collectAsStateWithLifecycle()
    val isContributingReserve by viewModel.isContributingReserve.collectAsStateWithLifecycle()
    val reserveContributionError by viewModel.reserveContributionError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val voiceManager = remember { VoiceSpeechManager(context) }
    val voiceState by voiceManager.state.collectAsStateWithLifecycle()

    var showVoiceModal by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            voiceManager.startListening()
        } else {
            voiceManager.onPermissionDenied()
        }
    }

    fun startVoiceFlow() {
        showVoiceModal = true
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            voiceManager.startListening()
        }
    }

    fun requestPermissionOrListen() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(voiceManager) {
        onDispose { voiceManager.reset() }
    }

    // Delegate voice transcription analysis to ViewModel
    androidx.compose.runtime.LaunchedEffect(voiceState) {
        val current = voiceState
        if (current is VoiceSpeechState.Success) {
            viewModel.onVoiceTranscriptionReceived(current.recognizedText)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KipuVoiceFab(
                        onClick = { startVoiceFlow() },
                    )
                    KipuRegisterFab(onClick = onRegisterCash)
                }
            },
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
                    if (insights.movementCount == 0 && insights.envelopeCount == 0 && state.monthlyReceipts.isEmpty()) {
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
                        val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
                        LazyColumn(
                            state = homeListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .kipuScrollbar(homeListState),
                            verticalArrangement = Arrangement.spacedBy(KipuLayout.sectionSpacing),
                            contentPadding = PaddingValues(
                                start = KipuLayout.screenHorizontalPadding,
                                end = KipuLayout.screenHorizontalPadding,
                                bottom = 88.dp,
                            ),
                        ) {
                            item(key = "home-header") {
                                KipuScreenHeader(
                                    title = "Tu dinero protegido",
                                    greeting = "Hola",
                                )
                            }
                            item(key = "daily-available-hero") {
                                DailyAvailableCard(
                                    cycleAvailable = insights.cycleAvailable,
                                    envelopeCount = insights.envelopeCount,
                                    periodSummary = insights.periodSummary,
                                    monthlyBudgetSummary = insights.monthlyBudgetSummary,
                                )
                            }
                            val reserveBalance = insights.reserveBalance
                            val availableBalance = insights.availableBalance
                            if (reserveBalance != null && availableBalance != null) {
                                item(key = "reserve-and-available") {
                                    ReserveAndAvailableCard(
                                        reserve = reserveBalance,
                                        available = availableBalance,
                                        monthlyTarget = insights.financialPlan?.reserveMonthlyContribution,
                                        hasContributedThisMonth = insights.hasCurrentMonthReserveContribution,
                                        isContributing = isContributingReserve,
                                        errorMessage = reserveContributionError,
                                        onContribute = viewModel::contributeMonthlyReserve,
                                    )
                                }
                            }
                            if (insights.antSpendingAlerts.isNotEmpty()) {
                                item(key = "ant-alerts-header") {
                                    KipuSectionHeader(title = "Gastos hormiga detectados")
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
                                        cycle = insights.cycleAvailable.cycle,
                                    )
                                }
                            }
                            insights.categoryDistribution?.let { distribution ->
                                if (!distribution.isEmpty) {
                                    item(key = "category-distribution") {
                                        CategoryDistributionCard(
                                            distribution = distribution,
                                            onCategoryClick = onNavigateToCategoryMovements,
                                        )
                                    }
                                }
                            }
                            if (state.monthlyReceipts.isNotEmpty()) {
                                item(key = "monthly-receipts") {
                                    MonthlyReceiptsCard(
                                        receipts = state.monthlyReceipts,
                                        financialPlan = insights.financialPlan,
                                        onMarkReceiptPaid = viewModel::markReceiptPaid,
                                        onUnmarkReceiptPaid = viewModel::unmarkReceiptPaid,
                                        onEditReceipts = {
                                            onNavigateToPlan("expenses")
                                        },
                                    )
                                }
                            }
                            insights.cashFlowSummary?.let { summary ->
                                item(key = "cash-flow-summary") {
                                    CashFlowSummaryCard(summary = summary)
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
                            if (insights.recentMovements.isNotEmpty()) {
                                item(key = "recent-header") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
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
                                    RecentMovementRow(
                                        movement = movement,
                                        categoryName = movement.categoryId?.let { state.categoryNamesById[it] },
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
    }

    if (showVoiceModal) {
        val categoryMap = (uiState as? HomeUiState.Content)?.categoryNamesById ?: emptyMap()
        val envelopes = (uiState as? HomeUiState.Content)?.envelopes.orEmpty()
        VoiceConfirmationBottomSheet(
            voiceState = voiceState,
            parsedIntent = parsedIntent,
            isAnalyzing = isAnalyzingVoice,
            isSaving = isSavingVoice,
            saveError = voiceSaveError,
            categoryNamesById = categoryMap,
            envelopes = envelopes,
            unexpectedExpenseState = voiceUnexpectedExpense,
            onStartListening = {
                viewModel.clearParsedVoiceIntent()
                requestPermissionOrListen()
            },
            onConfirmIntent = { intent, isUnexpected ->
                viewModel.saveVoiceIntent(intent, isUnexpected) {
                    showVoiceModal = false
                    voiceManager.reset()
                }
            },
            onUnexpectedAdjustmentToggled = viewModel::onVoiceUnexpectedAdjustmentToggled,
            onConfirmUnexpectedWithAdjustments = {
                viewModel.confirmVoiceUnexpectedExpense(applyAdjustments = true) {
                    showVoiceModal = false
                    voiceManager.reset()
                }
            },
            onConfirmUnexpectedWithoutAdjustments = {
                viewModel.confirmVoiceUnexpectedExpense(applyAdjustments = false) {
                    showVoiceModal = false
                    voiceManager.reset()
                }
            },
            onBackFromUnexpected = viewModel::dismissVoiceUnexpectedExpense,
            onDismiss = {
                viewModel.clearParsedVoiceIntent()
                showVoiceModal = false
                voiceManager.reset()
            },
        )
    }
}
}

@Composable
private fun ReserveAndAvailableCard(
    reserve: ReserveBalance,
    available: AvailableBalance,
    monthlyTarget: pe.kipu.core.domain.model.Money?,
    hasContributedThisMonth: Boolean,
    isContributing: Boolean,
    errorMessage: String?,
    onContribute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableIsNegative = available.availableBalance.signum() < 0
    KipuCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Saldo disponible acumulado: " +
                    formatPenAmountForDisplay(available.availableBalance.abs()) +
                    "; reserva para imprevistos: " + formatPenAmountForDisplay(reserve.balance.max(java.math.BigDecimal.ZERO))
            },
    ) {
        Text(
            text = "Tu saldo acumulado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Se conserva mientras no lo gastes. La reserva está separada del monto libre.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = "Disponible",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = (if (availableIsNegative) "- " else "") +
                        formatPenAmountForDisplay(available.availableBalance.abs()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (availableIsNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(
                    text = "Reserva para imprevistos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatPenAmountForDisplay(reserve.balance.max(java.math.BigDecimal.ZERO)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                monthlyTarget?.takeUnless { it.isZero() }?.let { target ->
                    Text(
                        text = "Meta mensual: ${formatPenAmountForDisplay(target.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        monthlyTarget?.takeUnless { it.isZero() }?.let {
            val hasEnoughAvailable = available.availableBalance >= it.amount
            if (hasContributedThisMonth) {
                Text(
                    text = "Aporte de este mes registrado",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                KipuSecondaryButton(
                    text = if (isContributing) "Registrando…" else "Aportar este mes",
                    onClick = onContribute,
                    enabled = !isContributing && hasEnoughAvailable,
                    fillWidth = true,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (!hasEnoughAvailable) {
                    Text(
                        text = "Registra ingresos o reduce gastos antes de hacer este aporte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics {
                        error(message)
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserCategoriesRow(
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
            AssistChip(
                onClick = { onCategoryClick(category.id) },
                label = {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            )
        }
    }
}

@Composable
private fun PeriodSummaryCard(
    summary: HomePeriodSummary,
    cycle: BudgetCycle,
    modifier: Modifier = Modifier,
) {
    KipuCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = HomeCycleText.periodTitle(cycle),
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
    categoryName: String? = null,
    modifier: Modifier = Modifier,
) {
    KipuCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MovementDisplayLabels.displayTitle(
                        movement.counterpartyName,
                        movement.description,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = RelativeDateFormatter.formatDayHeader(movement.recordedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = when (movement.channel) {
                            PaymentChannel.YAPE -> "Yape"
                            PaymentChannel.PLIN -> "Plin"
                            PaymentChannel.CASH -> "Efectivo"
                            PaymentChannel.MANUAL -> "Manual"
                            PaymentChannel.OTHER -> "Otro"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (categoryName != null) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
    monthlyBudgetSummary: MonthlyBudgetSummary? = null,
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
                    cycleAvailable.isOverBudget ->
                        HomeCycleText.overBudgetContentDescription(cycleAvailable.cycle)
                    availableAmount != null ->
                        "${HomeCycleText.heroHeader(cycleAvailable.cycle)}: S/ ${availableAmount.amount.toPlainString()}"
                    else -> "Disponible no calculado"
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = HomeCycleText.heroHeader(cycleAvailable.cycle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            
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
                        text = HomeCycleText.overBudgetMessage(cycleAvailable.cycle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                cycleAvailable.daysRemainingInCycle <= 0 -> {
                    Text(
                        text = HomeCycleText.noDaysRemaining(cycleAvailable.cycle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                else -> {
                    cycleAvailable.cycleAvailable?.let { amount ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatPenAmountForDisplay(amount.amount),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.W800,
                            )
                            KipuBadge(
                                text = HomeCycleText.remainingDays(
                                    cycle = cycleAvailable.cycle,
                                    days = cycleAvailable.daysRemainingInCycle,
                                ),
                                tone = KipuBadgeTone.Primary,
                            )
                        }
                    }
                }
            }
            
            // Dynamic High-Visibility Linear Progress Bar
            if (envelopeCount > 0 && !cycleAvailable.isOverBudget && cycleAvailable.daysRemainingInCycle > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        strokeCap = StrokeCap.Round,
                    )
                    if (periodSummary != null) {
                        val percentInt = (targetProgress * 100).toInt()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${formatPenAmountForDisplay(periodSummary.totalCycleSpent.amount)} gastados de " +
                                    formatPenAmountForDisplay(periodSummary.totalCycleLimit.amount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "$percentInt%",
                                style = MaterialTheme.typography.labelSmall,
                                color = progressColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            monthlyBudgetSummary?.let { summary ->
                MonthlyBudgetProgress(
                    summary = summary,
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
        }
    }
}

@Composable
private fun MonthlyBudgetProgress(
    summary: MonthlyBudgetSummary,
    modifier: Modifier = Modifier,
) {
    val progress = (summary.actualExpenses.amount.toFloat() / summary.plannedIncome.amount.toFloat())
        .coerceIn(0f, 1f)
    val progressColor = when {
        summary.isOverBudget || progress >= 0.9f -> MaterialTheme.colorScheme.error
        progress >= 0.75f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Presupuesto mensual: ${formatPenAmountForDisplay(summary.actualExpenses.amount)} gastados de ${formatPenAmountForDisplay(summary.plannedIncome.amount)}; quedan ${formatPenAmountForDisplay(summary.remaining.amount)}"
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Presupuesto mensual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatPenAmountForDisplay(summary.remaining.amount)} disponibles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                )
            }
            if (summary.isOverBudget) {
                KipuBadge(text = "Excedido", tone = KipuBadgeTone.Critical)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = "${formatPenAmountForDisplay(summary.actualExpenses.amount)} gastados de ${formatPenAmountForDisplay(summary.plannedIncome.amount)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}


@Composable
private fun AntSpendingAlertCard(
    alert: AntSpendingAlert,
    categoryName: String?,
    cycle: BudgetCycle,
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
            text = HomeAlertTranslator.toDisplayText(
                alert = alert,
                categoryName = categoryName,
                cycle = cycle,
            ),
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
    summary: CashFlowSummary,
    modifier: Modifier = Modifier,
) {
    val isPositive = summary.netCash.signum() >= 0
    val netColor = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    KipuCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            // Header: Title and Net Cash amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Balance y flujo de caja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Saldo acumulado según tus movimientos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isPositive) "+ " else "- ") + formatPenAmountForDisplay(summary.netCash.abs()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                    )
                    KipuBadge(
                        text = if (isPositive) "Favorable" else "En déficit",
                        tone = if (isPositive) KipuBadgeTone.Primary else KipuBadgeTone.Critical,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Income and Expense mini cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Total Income
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPenAmountForDisplay(summary.totalIncome.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Total Expense
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPenAmountForDisplay(summary.totalExpenses.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            if (summary.isGoalAtRisk) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "⚠️ Metas en riesgo: Tu efectivo disponible es menor que el ahorro planeado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
