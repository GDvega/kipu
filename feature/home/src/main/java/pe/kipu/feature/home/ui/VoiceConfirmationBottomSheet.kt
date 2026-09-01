package pe.kipu.feature.home.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuBadge
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuIconBadge
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuPurple
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.designsystem.voice.VoiceSpeechState
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.receipt.ServiceReceiptType
import pe.kipu.core.domain.voice.VoiceFinancialIntent
import pe.kipu.feature.home.presentation.VoiceUnexpectedExpenseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceConfirmationBottomSheet(
    voiceState: VoiceSpeechState,
    parsedIntent: VoiceFinancialIntent?,
    isAnalyzing: Boolean,
    isSaving: Boolean,
    saveError: String?,
    categoryNamesById: Map<String, String> = emptyMap(),
    envelopes: List<Envelope> = emptyList(),
    unexpectedExpenseState: VoiceUnexpectedExpenseState? = null,
    onStartListening: () -> Unit,
    onConfirmIntent: (VoiceFinancialIntent, Boolean) -> Unit,
    onUnexpectedAdjustmentToggled: (String) -> Unit = {},
    onConfirmUnexpectedWithAdjustments: () -> Unit = {},
    onConfirmUnexpectedWithoutAdjustments: () -> Unit = {},
    onBackFromUnexpected: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Asistente de Voz Kipu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (voiceState is VoiceSpeechState.Success || voiceState is VoiceSpeechState.Error) {
                    IconButton(onClick = onStartListening) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Volver a escuchar",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (voiceState) {
                is VoiceSpeechState.Listening -> {
                    ListeningView(rmsDb = voiceState.rmsDb, partialText = voiceState.partialText)
                }

                is VoiceSpeechState.Processing -> {
                    ProcessingView()
                }

                is VoiceSpeechState.Success -> {
                    if (unexpectedExpenseState != null) {
                        VoiceUnexpectedExpensePlanView(
                            state = unexpectedExpenseState,
                            onAdjustmentToggled = onUnexpectedAdjustmentToggled,
                            onConfirmWithAdjustments = onConfirmUnexpectedWithAdjustments,
                            onConfirmWithoutAdjustments = onConfirmUnexpectedWithoutAdjustments,
                            onBack = onBackFromUnexpected,
                        )
                    } else if (isAnalyzing) {
                        ProcessingView()
                    } else if (parsedIntent != null && parsedIntent !is VoiceFinancialIntent.Unknown) {
                        ParsedIntentView(
                            intent = parsedIntent,
                            categoryNamesById = categoryNamesById,
                            envelopes = envelopes,
                            isSaving = isSaving,
                            saveError = saveError,
                            onConfirm = onConfirmIntent,
                            onRetry = onStartListening,
                        )
                    } else {
                        UnrecognizedView(
                            rawText = voiceState.recognizedText,
                            onRetry = onStartListening,
                        )
                    }
                }

                is VoiceSpeechState.Error -> {
                    ErrorView(
                        errorMessage = voiceState.message,
                        onRetry = onStartListening,
                    )
                }

                VoiceSpeechState.Idle -> {
                    IdleView(onStartListening = onStartListening)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ListeningView(rmsDb: Float, partialText: String) {
    val pulseSize by animateFloatAsState(
        targetValue = (56 + rmsDb * 3).coerceIn(56f, 88f),
        animationSpec = tween(150),
        label = "pulseAnimation",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(pulseSize.dp)
                    .clip(CircleShape)
                    .background(KipuPrimary.copy(alpha = 0.2f)),
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(KipuPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Escuchando",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Text(
            text = "Te estoy escuchando...",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = KipuPrimary,
        )

        if (partialText.isNotBlank()) {
            Text(
                text = "“$partialText”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Text(
                text = "Ejemplos:\n• “Gasté 5 soles en un agua”\n• “Pagué 15 soles de taxi”\n• “Guardé 200 soles para mi meta carro”\n• “Ya pagué mi recibo de luz”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun ProcessingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 24.dp),
    ) {
        KipuLoadingIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Interpretando tu comando...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ParsedIntentView(
    intent: VoiceFinancialIntent,
    categoryNamesById: Map<String, String>,
    envelopes: List<Envelope>,
    isSaving: Boolean,
    saveError: String?,
    onConfirm: (VoiceFinancialIntent, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    var selectedCategoryId by remember(intent) {
        mutableStateOf(
            if (intent is VoiceFinancialIntent.Expense) intent.categoryId else CategoryIds.OTHER
        )
    }
    var selectedEnvelopeId by remember(intent, envelopes) {
        mutableStateOf(
            (intent as? VoiceFinancialIntent.Expense)?.envelopeId
                ?: envelopes.filter { it.categoryId == selectedCategoryId }.singleOrNull()?.id,
        )
    }
    var isUnexpectedExpense by remember(intent) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Speech Bubble
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "🗣️ “${intent.rawText}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(12.dp),
            )
        }

        // Preview Card
        KipuCard(modifier = Modifier.fillMaxWidth()) {
            when (intent) {
                is VoiceFinancialIntent.Expense -> {
                    IntentDetailRow(
                        badgeIcon = categoryIconFor(selectedCategoryId),
                        badgeTint = categoryTintFor(selectedCategoryId),
                        badgeText = "Gasto (${categoryDisplayName(selectedCategoryId, categoryNamesById)})",
                        badgeTone = KipuBadgeTone.Critical,
                        title = intent.description,
                        subtitle = "Canal: ${intent.channel.name}",
                        amount = intent.amount,
                        amountType = AmountType.EXPENSE,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Categoría del gasto:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val defaultCategories: List<Pair<String, String>> = listOf(
                            CategoryIds.FOOD to "Comida",
                            CategoryIds.TRANSPORT to "Transporte",
                            CategoryIds.SERVICES to "Servicios",
                            CategoryIds.OTHER to "Gastos hormiga / Otros",
                        )
                        val allCategories: List<Pair<String, String>> = buildList {
                            addAll(defaultCategories)
                            val defaultIds = defaultCategories.map { it.first }.toSet()
                            categoryNamesById.forEach { (id, name) ->
                                if (id !in defaultIds) {
                                    add(id to name)
                                }
                            }
                        }

                        allCategories.forEach { (catId, catName) ->
                            val isSelected = catId == selectedCategoryId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryId = catId
                                    selectedEnvelopeId = envelopes
                                        .filter { it.categoryId == catId }
                                        .singleOrNull()
                                        ?.id
                                },
                                label = {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                } else null,
                            )
                        }
                    }

                    if (envelopes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sobre del plan (opcional):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(
                                selected = selectedEnvelopeId == null,
                                onClick = { selectedEnvelopeId = null },
                                label = { Text("Ninguno") },
                            )
                            envelopes.forEach { envelope ->
                                FilterChip(
                                    selected = selectedEnvelopeId == envelope.id,
                                    onClick = {
                                        selectedEnvelopeId = envelope.id
                                        selectedCategoryId = envelope.categoryId
                                    },
                                    label = { Text(envelope.name) },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isUnexpectedExpense,
                                enabled = !isSaving,
                                role = Role.Switch,
                                onValueChange = { isUnexpectedExpense = it },
                            )
                            .padding(top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Es una compra imprevista", fontWeight = FontWeight.Bold)
                            Text(
                                "Verás la cobertura y un reajuste antes de guardar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isUnexpectedExpense,
                            onCheckedChange = null,
                            enabled = !isSaving,
                        )
                    }
                }

                is VoiceFinancialIntent.Income -> {
                    IntentDetailRow(
                        badgeIcon = Icons.AutoMirrored.Filled.TrendingUp,
                        badgeTint = KipuPrimary,
                        badgeText = "Ingreso",
                        badgeTone = KipuBadgeTone.Primary,
                        title = intent.description,
                        subtitle = "Canal: ${intent.channel.name}",
                        amount = intent.amount,
                        amountType = AmountType.INCOME,
                    )
                }

                is VoiceFinancialIntent.GoalContribution -> {
                    IntentDetailRow(
                        badgeIcon = Icons.Default.Savings,
                        badgeTint = KipuPurple,
                        badgeText = "Abono a Meta",
                        badgeTone = KipuBadgeTone.Purple,
                        title = "Meta: ${intent.goalQuery}",
                        subtitle = "Ahorro acumulado",
                        amount = intent.amount,
                        amountType = AmountType.EXPENSE,
                    )
                }

                is VoiceFinancialIntent.ServiceReceiptPayment -> {
                    IntentDetailRow(
                        badgeIcon = intent.serviceIcon(),
                        badgeTint = KipuAmber,
                        badgeText = "Recibo del Mes",
                        badgeTone = KipuBadgeTone.Warning,
                        title = "Pago ${intent.serviceKey.defaultTitle}",
                        subtitle = "Se marcará como pagado este mes",
                        amount = intent.amount,
                        amountType = AmountType.EXPENSE,
                    )
                }

                is VoiceFinancialIntent.Unknown -> Unit
            }
        }

        if (saveError != null) {
            Text(
                text = saveError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics {
                    error(saveError)
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }

        KipuPrimaryButton(
            text = if (isSaving) "Guardando..." else "Confirmar y Guardar",
            onClick = {
                val finalIntent = if (intent is VoiceFinancialIntent.Expense) {
                    intent.copy(categoryId = selectedCategoryId, envelopeId = selectedEnvelopeId)
                } else {
                    intent
                }
                onConfirm(finalIntent, isUnexpectedExpense)
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        KipuSecondaryButton(
            text = "Hablar de nuevo",
            onClick = onRetry,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VoiceUnexpectedExpensePlanView(
    state: VoiceUnexpectedExpenseState,
    onAdjustmentToggled: (String) -> Unit,
    onConfirmWithAdjustments: () -> Unit,
    onConfirmWithoutAdjustments: () -> Unit,
    onBack: () -> Unit,
) {
    val coverage = state.preview.coverage
    val selectedPlan = state.selectedRecoveryPlan
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Revisa esta compra imprevista", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Kipu no moverá tu dinero. Tú confirmas cómo quedará registrado.")
        VoiceCoverageLine("De tu reserva", coverage.fromReserve.amount)
        VoiceCoverageLine("De tu saldo disponible", coverage.fromAvailableBalance.amount)
        VoiceCoverageLine("Aún por compensar", coverage.uncovered.amount)

        if (state.preview.recoveryPlan.adjustments.isNotEmpty()) {
            Text("Reajuste opcional", fontWeight = FontWeight.Bold)
            Text(
                "Comida y transporte permanecen protegidos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.preview.recoveryPlan.adjustments.forEach { adjustment ->
                val selected = adjustment.envelopeId in state.selectedEnvelopeIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = selected,
                            enabled = !state.isSaving,
                            role = Role.Checkbox,
                            onValueChange = { onAdjustmentToggled(adjustment.envelopeId) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = selected, onCheckedChange = null, enabled = !state.isSaving)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(adjustment.envelopeName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Reducir ${formatPenAmountForDisplay(adjustment.reduction.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Text(
                if (selectedPlan.remainingGap.isZero()) {
                    "El reajuste seleccionado cubre el faltante."
                } else {
                    "Aún faltará compensar ${formatPenAmountForDisplay(selectedPlan.remainingGap.amount)}."
                },
                color = if (selectedPlan.remainingGap.isZero()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics {
                    error(message)
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        KipuPrimaryButton(
            text = when {
                state.isSaving -> "Guardando..."
                state.preview.recoveryPlan.adjustments.isEmpty() -> "Guardar compra"
                else -> "Guardar y reajustar"
            },
            onClick = if (state.preview.recoveryPlan.adjustments.isEmpty()) {
                onConfirmWithoutAdjustments
            } else {
                onConfirmWithAdjustments
            },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.preview.recoveryPlan.adjustments.isNotEmpty()) {
            KipuSecondaryButton(
                text = "Guardar sin reajustar sobres",
                onClick = onConfirmWithoutAdjustments,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        KipuSecondaryButton(
            text = "Volver",
            onClick = onBack,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VoiceCoverageLine(label: String, amount: java.math.BigDecimal) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(formatPenAmountForDisplay(amount), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IntentDetailRow(
    badgeIcon: ImageVector,
    badgeTint: Color,
    badgeText: String,
    badgeTone: KipuBadgeTone,
    title: String,
    subtitle: String,
    amount: Money?,
    amountType: AmountType,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KipuIconBadge(
            icon = badgeIcon,
            tint = badgeTint,
            size = 48.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
            KipuBadge(text = badgeText, tone = badgeTone)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (amount != null) {
            KipuAmountText(
                amount = amount.amount,
                type = amountType,
            )
        }
    }
}

@Composable
private fun UnrecognizedView(rawText: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        Text(
            text = "Escuché: “$rawText”",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "No pudimos identificar el monto o el tipo de movimiento. Por favor sé más específico con el monto.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        KipuPrimaryButton(
            text = "Intentar de nuevo",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorView(errorMessage: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        KipuPrimaryButton(
            text = "Presiona para hablar",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun IdleView(onStartListening: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp),
    ) {
        IconButton(
            onClick = onStartListening,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(KipuPrimary),
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Hablar",
                tint = Color.Black,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = "Toca el micrófono para dictar tus gastos, ingresos, metas o recibos",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Kipu interpreta la transcripción en tu dispositivo; revisarás el resultado " +
                "antes de guardarlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun categoryIconFor(categoryId: String): ImageVector = when (categoryId) {
    CategoryIds.FOOD -> Icons.Filled.ShoppingCart
    CategoryIds.TRANSPORT -> Icons.Filled.Place
    CategoryIds.SERVICES -> Icons.Filled.Receipt
    CategoryIds.OTHER -> Icons.Filled.Savings
    else -> Icons.Filled.ShoppingCart
}

private fun categoryTintFor(categoryId: String): Color = when (categoryId) {
    CategoryIds.FOOD -> KipuRed
    CategoryIds.TRANSPORT -> KipuAmber
    CategoryIds.SERVICES -> KipuPrimary
    CategoryIds.OTHER -> KipuPurple
    else -> KipuRed
}

private fun categoryDisplayName(categoryId: String, categoryNamesById: Map<String, String>): String {
    return categoryNamesById[categoryId] ?: when (categoryId) {
        CategoryIds.FOOD -> "Comida"
        CategoryIds.TRANSPORT -> "Transporte"
        CategoryIds.SERVICES -> "Servicios"
        CategoryIds.OTHER -> "Gastos hormiga / Otros"
        else -> "General"
    }
}

private fun VoiceFinancialIntent.ServiceReceiptPayment.serviceIcon(): ImageVector = when (serviceKey.type) {
    ServiceReceiptType.LIGHT -> Icons.Filled.Bolt
    ServiceReceiptType.WATER -> Icons.Filled.WaterDrop
    ServiceReceiptType.INTERNET -> Icons.Filled.Wifi
    ServiceReceiptType.DEBTS -> Icons.Filled.Handshake
    else -> Icons.Filled.Receipt
}
