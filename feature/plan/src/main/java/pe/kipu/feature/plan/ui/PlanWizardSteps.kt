package pe.kipu.feature.plan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text




import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuAlertCard
import pe.kipu.core.designsystem.component.KipuAlertTone
import pe.kipu.core.designsystem.component.KipuAmountPresetRow
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuCardStyle
import pe.kipu.core.designsystem.component.KipuFilterChip
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.component.KipuHeroCard
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuSelectionCard
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.AntSpendingQuickCategories
import pe.kipu.core.domain.plan.FixedExpenseBreakdownCalculator
import pe.kipu.core.domain.plan.FixedExpenseQuickCategories
import pe.kipu.core.domain.plan.GoalTimeframeOptions
import pe.kipu.core.domain.plan.GoalType
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.plan.PeruPlanDefaults
import pe.kipu.core.domain.plan.PlanEnvelopeTemplates
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.plan.PlanWizardLineItem
import pe.kipu.core.domain.plan.currency
import pe.kipu.core.domain.plan.label
import pe.kipu.core.domain.plan.subtitle
import pe.kipu.core.domain.plan.title
import pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCase
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.feature.plan.presentation.estimateMonthlyIncome
import pe.kipu.feature.plan.presentation.PlanWizardStep
import pe.kipu.feature.plan.presentation.PlanWizardCycleText
import pe.kipu.feature.plan.presentation.PlanWizardUiState
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun IncomeStepContent(
    state: PlanWizardUiState.Content,
    onProfileSelected: (IncomeProfile) -> Unit,
    onFixedBaseChanged: (String) -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onSecondQuincenaChanged: (String) -> Unit,
    onPayFrequencySelected: (PayFrequency) -> Unit,
    onLowWeekChanged: (String) -> Unit,
    onNormalWeekChanged: (String) -> Unit,
    onGoodWeekChanged: (String) -> Unit,
    onApproximateIncomeChanged: (String) -> Unit,
    onAddIncomeLine: () -> Unit,
    onRemoveIncomeLine: (String) -> Unit,
    onIncomeLineChanged: (String, String, String) -> Unit,
    onSkipApproximate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Saldo inicial opcional ──────────────────────────────────────────
        KipuCard(style = KipuCardStyle.Large) {
            Column {
                Text(
                    text = "¿Tienes plata disponible ahorita?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "No importa si empezaste el mes hace rato. Pon lo que tienes ahora mismo en efectivo, Yape o banco — o déjalo en cero si prefieres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                KipuPenOutlinedTextField(
                    value = state.initialBalanceText,
                    onValueChange = onInitialBalanceChanged,
                    label = "Saldo disponible hoy (opcional)",
                    placeholder = "0",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    pe.kipu.core.designsystem.component.KipuTextLink(
                        text = "No sé / saltar",
                        onClick = { onInitialBalanceChanged("0") },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IncomeProfile.entries.forEach { profile ->
                KipuSelectionCard(
                    title = profile.title(),
                    subtitle = profile.subtitle(),
                    selected = state.incomeProfile == profile,
                    onClick = { onProfileSelected(profile) },
                    leading = { IncomeProfileIcon(profile) },
                )
            }
        }

        when (state.incomeProfile) {
            IncomeProfile.FIXED -> {
                Text(
                    text = "Frecuencia",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                KipuFilterChipRow(
                    labels = PayFrequency.entries.map { it.label() },
                    selectedIndex = PayFrequency.entries.indexOf(state.payFrequency),
                    onSelected = { index -> onPayFrequencySelected(PayFrequency.entries[index]) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                )
                when (state.payFrequency) {
                    PayFrequency.BIWEEKLY -> {
                        KipuPenOutlinedTextField(
                            value = state.fixedBaseText,
                            onValueChange = onFixedBaseChanged,
                            label = "1ra quincena",
                            placeholder = PeruPlanDefaults.SEED_BIWEEKLY_FIRST.stripTrailingZeros().toPlainString(),
                        )
                        KipuPenOutlinedTextField(
                            value = state.secondQuincenaText,
                            onValueChange = onSecondQuincenaChanged,
                            label = "2da quincena",
                            placeholder = PeruPlanDefaults.SEED_BIWEEKLY_SECOND.stripTrailingZeros().toPlainString(),
                        )
                    }
                    PayFrequency.WEEKLY -> {
                        KipuPenOutlinedTextField(
                            value = state.fixedBaseText,
                            onValueChange = onFixedBaseChanged,
                            label = "Sueldo semanal",
                            placeholder = PeruPlanDefaults.SEED_WEEKLY_FIXED.stripTrailingZeros().toPlainString(),
                        )
                    }
                    PayFrequency.MONTHLY -> {
                        KipuPenOutlinedTextField(
                            value = state.fixedBaseText,
                            onValueChange = onFixedBaseChanged,
                            label = "Sueldo mensual (aproximado)",
                            placeholder = PeruPlanDefaults.SEED_MONTHLY_FIXED.stripTrailingZeros().toPlainString(),
                        )
                    }
                }
                Text(
                    text = "Otros ingresos del mes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                state.additionalIncomeLines.forEach { line ->
                    WizardLineItemRow(
                        label = line.label,
                        amountText = line.amountText,
                        labelPlaceholder = "Ej. Freelance",
                        onLabelChanged = { onIncomeLineChanged(line.id, it, line.amountText) },
                        onAmountChanged = { onIncomeLineChanged(line.id, line.label, it) },
                        onRemove = { onRemoveIncomeLine(line.id) },
                    )
                }
                KipuSecondaryButton(
                    text = "+ Agregar otro ingreso",
                    onClick = onAddIncomeLine,
                    fillWidth = true,
                )
            }

            IncomeProfile.VARIABLE -> {
                KipuPenOutlinedTextField(
                    value = state.lowWeekText,
                    onValueChange = onLowWeekChanged,
                    label = "Semana baja",
                    placeholder = PeruPlanDefaults.SEED_VARIABLE_LOW_WEEK.stripTrailingZeros().toPlainString(),
                )
                KipuPenOutlinedTextField(
                    value = state.normalWeekText,
                    onValueChange = onNormalWeekChanged,
                    label = "Semana normal",
                    placeholder = PeruPlanDefaults.SEED_VARIABLE_NORMAL_WEEK.stripTrailingZeros().toPlainString(),
                )
                KipuPenOutlinedTextField(
                    value = state.goodWeekText,
                    onValueChange = onGoodWeekChanged,
                    label = "Semana buena",
                    placeholder = PeruPlanDefaults.SEED_VARIABLE_GOOD_WEEK.stripTrailingZeros().toPlainString(),
                )
            }

            IncomeProfile.APPROXIMATE -> {
                KipuPenOutlinedTextField(
                    value = state.approximateIncomeText,
                    onValueChange = onApproximateIncomeChanged,
                    label = "Aproximado mensual",
                    placeholder = "1500",
                )
                KipuAlertCard(tone = KipuAlertTone.Info) {
                    Text(
                        text = "No hay problema. Kipu usará este monto como base y podrás ajustarlo " +
                            "después con tus movimientos reales.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    KipuTextLink(text = "Usar ejemplo S/ 1500", onClick = onSkipApproximate)
                }
            }
        }
    }
}

@Composable
private fun IncomeProfileIcon(profile: IncomeProfile) {
    val icon = when (profile) {
        IncomeProfile.FIXED -> Icons.Default.Work
        IncomeProfile.VARIABLE -> Icons.Default.Bolt
        IncomeProfile.APPROXIMATE -> Icons.Default.Home
    }
    val tint = when (profile) {
        IncomeProfile.FIXED -> MaterialTheme.colorScheme.primary
        IncomeProfile.VARIABLE -> MaterialTheme.colorScheme.tertiary
        IncomeProfile.APPROXIMATE -> MaterialTheme.colorScheme.secondary
    }
    IconBadge(icon = icon, tint = tint)
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixedExpensesStepContent(
    electricityText: String,
    waterText: String,
    internetText: String,
    rentText: String,
    phoneText: String,
    debtsText: String,
    educationText: String,
    customExpenseLines: List<PlanWizardLineItem>,
    onElectricityChanged: (String) -> Unit,
    onWaterChanged: (String) -> Unit,
    onInternetChanged: (String) -> Unit,
    onRentChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onDebtsChanged: (String) -> Unit,
    onEducationChanged: (String) -> Unit,
    onCustomLineChanged: (String, String, String) -> Unit,
    onAddCustomExpenseLine: () -> Unit,
    onRemoveCustomExpenseLine: (String) -> Unit,
    onQuickExpenseSelected: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val totalText = FixedExpenseBreakdownCalculator.formatTotal(
        electricityText = electricityText,
        waterText = waterText,
        internetText = internetText,
        rentText = rentText,
        phoneText = phoneText,
        debtsText = debtsText,
        educationText = educationText,
        customLines = customExpenseLines,
    )
    val subscriptionSuggestions = FixedExpenseQuickCategories.SUBSCRIPTION_SUGGESTIONS

    KipuCard(style = KipuCardStyle.Large) {
        Text(
            text = "Pagos sí o sí (gastos fijos del mes)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Pon un aproximado para planificar. Cuando pagues luz, agua, gas u otro servicio registrarás el monto real.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        FixedExpenseRow(Icons.Default.Bolt, "Luz", electricityText, Color(0xFFF59E0B), onElectricityChanged)
        FixedExpenseRow(Icons.Default.WaterDrop, "Agua", waterText, Color(0xFF0284C7), onWaterChanged)
        FixedExpenseRow(Icons.Default.Wifi, "Internet", internetText, Color(0xFF06B6D4), onInternetChanged)
        FixedExpenseRow(Icons.Default.Home, "Alquiler / casa", rentText, Color(0xFFF97316), onRentChanged)
        FixedExpenseRow(Icons.Default.PhoneAndroid, "Celular", phoneText, Color(0xFF10B981), onPhoneChanged)
        FixedExpenseRow(Icons.Default.Handshake, "Préstamos y deudas", debtsText, Color(0xFFEF4444), onDebtsChanged)
        FixedExpenseRow(Icons.Default.School, "Universidad / educación", educationText, Color(0xFF8B5CF6), onEducationChanged)

        customExpenseLines.forEach { line ->
            WizardLineItemRow(
                label = line.label,
                amountText = line.amountText,
                labelPlaceholder = "Nombre (ej. Gas, Netflix)",
                onLabelChanged = { onCustomLineChanged(line.id, it, line.amountText) },
                onAmountChanged = { onCustomLineChanged(line.id, line.label, it) },
                onRemove = { onRemoveCustomExpenseLine(line.id) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        KipuSecondaryButton(
            text = "+ Agregar otro gasto fijo",
            onClick = onAddCustomExpenseLine,
            modifier = Modifier.padding(top = 12.dp),
            fillWidth = true,
        )

        val availableSuggestions = subscriptionSuggestions.filterNot { suggestion ->
            customExpenseLines.any { it.label.equals(suggestion, ignoreCase = true) }
        }

        if (availableSuggestions.isNotEmpty()) {
            Text(
                text = "Suscripciones o pagos recurrentes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableSuggestions.forEach { suggestion ->
                    KipuFilterChip(
                        text = "+ $suggestion",
                        selected = false,
                        onClick = { onQuickExpenseSelected(suggestion) },
                    )
                }
            }
        }
    }

    KipuCard(
        modifier = Modifier.padding(top = KipuLayout.sectionSpacing),
        style = KipuCardStyle.Large,
    ) {
        Text(text = "Total pagos sí o sí", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = if (totalText.isNotBlank()) "S/ $totalText" else "S/ 0",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Monto de referencia para calcular tu plan general",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        KipuTextLink(text = "No tengo gastos fijos", onClick = onSkip)
    }
}


@Composable
private fun FixedExpenseRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon = icon, tint = tint)
        KipuPenOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = "Ej. S/ 150",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun EnvelopesStepContent(
    budgetCycle: pe.kipu.core.domain.model.BudgetCycle,
    envelopeLimits: Map<String, String>,
    customizingEnvelopeId: String?,
    customEnvelopeLines: List<PlanWizardLineItem>,
    reserveMonthlyContributionText: String,
    onBudgetCycleSelected: (pe.kipu.core.domain.model.BudgetCycle) -> Unit,
    onReserveMonthlyContributionChanged: (String) -> Unit,
    onPresetSelected: (String, BigDecimal) -> Unit,
    onLimitChanged: (String, String) -> Unit,
    onCustomize: (String?) -> Unit,
    onAddCustomEnvelope: () -> Unit,
    onRemoveCustomEnvelope: (String) -> Unit,
    onCustomEnvelopeChanged: (String, String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KipuCard(style = KipuCardStyle.Large) {
            Text(
                text = "¿Cómo quieres llevar tu presupuesto?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val cycleLabels = listOf("Diario", "Semanal", "Mensual")
            val cycleValues = listOf(pe.kipu.core.domain.model.BudgetCycle.DAILY, pe.kipu.core.domain.model.BudgetCycle.WEEKLY, pe.kipu.core.domain.model.BudgetCycle.MONTHLY)
            KipuFilterChipRow(
                labels = cycleLabels,
                selectedIndex = cycleValues.indexOf(budgetCycle),
                onSelected = { index -> onBudgetCycleSelected(cycleValues[index]) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            )
            Text(
                text = when (budgetCycle) {
                    pe.kipu.core.domain.model.BudgetCycle.DAILY -> "Establecerás límites y metas para cada día."
                    pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> "Establecerás límites y metas para cada semana."
                    pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> "Establecerás límites y metas para todo el mes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        KipuCard(style = KipuCardStyle.Large) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Default.Thunderstorm, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Reserva para imprevistos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Aparta un monto cada mes. Si no lo usas, se acumula para una compra o emergencia futura.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            KipuPenOutlinedTextField(
                value = reserveMonthlyContributionText,
                onValueChange = onReserveMonthlyContributionChanged,
                label = "Aporte mensual para imprevistos",
                placeholder = "0",
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "No se registra como gasto: queda separado dentro de tu saldo hasta que confirmes usarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        val periodLabel = when (budgetCycle) {
            pe.kipu.core.domain.model.BudgetCycle.DAILY -> "por día"
            pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> "por semana"
            pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> "por mes"
        }

        PlanEnvelopeTemplates.WIZARD_ENVELOPES.forEach { template ->
            val limitText = envelopeLimits[template.envelopeId].orEmpty()
            val selectedAmount = limitText.toBigDecimalOrNull()
            val icon = when (template.name) {
                "Comida" -> Icons.Default.Fastfood
                "Transporte" -> Icons.Default.DirectionsCar
                "Ocio" -> Icons.Default.SportsEsports
                else -> Icons.Default.Groups
            }

            KipuCard(style = KipuCardStyle.Large) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = icon, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = template.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val cyclePresets = PlanEnvelopeTemplates.presetsForCycle(template, budgetCycle)
                KipuAmountPresetRow(
                    presets = cyclePresets,
                    selectedAmount = selectedAmount,
                    onPresetSelected = { onPresetSelected(template.envelopeId, it) },
                    modifier = Modifier.selectableGroup(),
                    onCustomize = { onCustomize(template.envelopeId) },
                )
                if (customizingEnvelopeId == template.envelopeId) {
                    KipuPenOutlinedTextField(
                        value = limitText,
                        onValueChange = { onLimitChanged(template.envelopeId, it) },
                        label = "Monto personalizado $periodLabel",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (selectedAmount != null && selectedAmount.signum() > 0) {
                    val monthlyEquivalent = when (budgetCycle) {
                        pe.kipu.core.domain.model.BudgetCycle.DAILY -> selectedAmount.multiply(BigDecimal("30"))
                        pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> selectedAmount.multiply(BigDecimal("4"))
                        pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> selectedAmount
                    }
                    Text(
                        text = when (budgetCycle) {
                            pe.kipu.core.domain.model.BudgetCycle.DAILY -> "S/ ${selectedAmount.stripTrailingZeros().toPlainString()} al día · equivale a S/ ${monthlyEquivalent.stripTrailingZeros().toPlainString()} al mes"
                            pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> "S/ ${selectedAmount.stripTrailingZeros().toPlainString()} por semana · equivale a S/ ${monthlyEquivalent.stripTrailingZeros().toPlainString()} al mes"
                            pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> "S/ ${selectedAmount.stripTrailingZeros().toPlainString()} al mes"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        if (customEnvelopeLines.isNotEmpty()) {
            Text(
                text = "Tus sobres personalizados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        customEnvelopeLines.forEach { line ->
            KipuCard(style = KipuCardStyle.Large) {
                WizardLineItemRow(
                    label = line.label,
                    amountText = line.amountText,
                    labelPlaceholder = "Nombre (ej. Mascotas)",
                    onLabelChanged = { onCustomEnvelopeChanged(line.id, it, line.amountText) },
                    onAmountChanged = { onCustomEnvelopeChanged(line.id, line.label, it) },
                    onRemove = { onRemoveCustomEnvelope(line.id) },
                )
            }
        }

        KipuSecondaryButton(
            text = "+ Agregar sobre",
            onClick = onAddCustomEnvelope,
            fillWidth = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AntSpendingStepContent(
    budgetCycle: BudgetCycle,
    categories: List<Category>,
    limitText: String,
    selectedCategoryIds: Set<String>,
    pendingCategoryName: String,
    alertEnabled: Boolean,
    onLimitChanged: (String) -> Unit,
    onPresetSelected: (BigDecimal) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onPendingCategoryNameChanged: (String) -> Unit,
    onAddAntCategory: () -> Unit,
    onQuickAntCategorySelected: (String) -> Unit,
    onAlertToggled: (Boolean) -> Unit,
) {
    val antSuggestions = AntSpendingQuickCategories.SUGGESTIONS
    val antPresets = PlanEnvelopeTemplates.antSpendingPresetsForCycle(budgetCycle)
    val defaultAntLimit = antPresets[1]
    val limitAmount = limitText.toBigDecimalOrNull() ?: defaultAntLimit
    val alertAt80 = limitAmount.multiply(BigDecimal("0.8")).setScale(0, RoundingMode.HALF_UP)
    val (minVal, maxVal, steps) = when (budgetCycle) {
        BudgetCycle.DAILY -> Triple(2f, 50f, 24)
        BudgetCycle.WEEKLY -> Triple(10f, 100f, 18)
        BudgetCycle.MONTHLY -> Triple(40f, 400f, 18)
    }

    Column {
        KipuCard(style = KipuCardStyle.Large) {
            Text(
                text = "¿Qué son los gastos hormiga?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Son pequeños consumos diarios (cafecito, gaseosa, snacks, antojitos de paso, taxi por apuro, propinas, comisiones) que parecen no importar, pero al sumarlos a fin de mes representan una fuga importante de dinero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Text(
                text = "Elige qué gastos pequeños se te escapan más:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    KipuFilterChip(
                        text = category.name,
                        selected = category.id in selectedCategoryIds,
                        onClick = { onCategoryToggled(category.id) },
                    )
                }
            }
            Text(
                text = "Sugerencias frecuentes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                antSuggestions.forEach { suggestion ->
                    KipuFilterChip(
                        text = suggestion,
                        selected = categories.any {
                            it.name.equals(suggestion, ignoreCase = true) && it.id in selectedCategoryIds
                        },
                        onClick = { onQuickAntCategorySelected(suggestion) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KipuPenOutlinedTextField(
                    value = pendingCategoryName,
                    onValueChange = onPendingCategoryNameChanged,
                    label = "Otra categoría",
                    showPrefix = false,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
                KipuSecondaryButton(
                    text = "Agregar",
                    onClick = onAddAntCategory,
                )
            }
        }

        KipuCard(modifier = Modifier.padding(top = KipuLayout.sectionSpacing), style = KipuCardStyle.Large) {
            Text(
                text = PlanWizardCycleText.antSpendingLimitTitle(budgetCycle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "S/ ${limitAmount.stripTrailingZeros().toPlainString()}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Slider(
                value = limitAmount.toFloat().coerceIn(minVal, maxVal),
                onValueChange = { onLimitChanged(it.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toPlainString()) },
                valueRange = minVal..maxVal,
                steps = steps,
            )
            KipuAmountPresetRow(
                presets = antPresets,
                selectedAmount = limitAmount,
                onPresetSelected = onPresetSelected,
                modifier = Modifier.selectableGroup(),
            )
            KipuAlertCard(tone = KipuAlertTone.Info, modifier = Modifier.padding(top = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = alertEnabled,
                            role = Role.Switch,
                            onValueChange = onAlertToggled,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ALERTA ANTI-HORMIGA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Avisarme al 80%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Switch(
                        checked = alertEnabled,
                        onCheckedChange = null,
                    )
                }
                if (alertEnabled) {
                    Text(
                        text = "Cuando lleves S/ ${alertAt80.toPlainString()} en gastos pequeños",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text(
                        text = "Puedes activarla cuando quieras desde el wizard o perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun GoalStepContent(
    state: PlanWizardUiState.Content,
    onGoalTypeSelected: (GoalType) -> Unit,
    onGoalNameChanged: (String) -> Unit,
    onGoalTargetChanged: (String) -> Unit,
    onGoalCurrentChanged: (String) -> Unit,
    onGoalMonthsChanged: (String) -> Unit,
    onSocialDebtToggled: (Boolean) -> Unit,
    onSocialDebtCounterpartyChanged: (String) -> Unit,
    onSocialDebtAmountChanged: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val goalCurrency = state.goalType.currency()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KipuCard(style = KipuCardStyle.Large) {
            Text(text = "Tipo de meta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            KipuFilterChipRow(
                labels = GoalType.entries.map { it.label() },
                selectedIndex = GoalType.entries.indexOf(state.goalType),
                onSelected = { index -> onGoalTypeSelected(GoalType.entries[index]) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            )
        }

        KipuPenOutlinedTextField(
            value = state.goalName,
            onValueChange = onGoalNameChanged,
            label = "Nombre de la meta",
            showPrefix = false,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
        )
        KipuPenOutlinedTextField(
            value = state.goalTargetText,
            onValueChange = onGoalTargetChanged,
            label = "¿Cuánto necesitas?",
            placeholder = if (goalCurrency.code == "USD") "200" else "1000",
            currencyPrefix = "${goalCurrency.symbol} ",
        )
        KipuPenOutlinedTextField(
            value = state.goalCurrentText,
            onValueChange = onGoalCurrentChanged,
            label = "¿Cuánto ya tienes?",
            placeholder = if (goalCurrency.code == "USD") "50" else "150",
            currencyPrefix = "${goalCurrency.symbol} ",
        )

        Text(text = "¿Para cuándo lo quieres?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        KipuPenOutlinedTextField(
            value = state.goalMonthsText,
            onValueChange = onGoalMonthsChanged,
            label = "Cantidad de meses",
            currencyPrefix = "meses",
            showPrefix = false, // We'll put "meses" in the placeholder or label to avoid prefix confusion
            placeholder = "Ej. 6",
        )

        state.suggestedGoalWeekly?.let { weekly ->
            KipuAlertCard(tone = KipuAlertTone.Info) {
                Text(text = "KIPU SUGIERE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "${goalCurrency.symbol} ${weekly.amount.stripTrailingZeros().toPlainString()} por semana",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val target = state.goalTargetText.ifBlank { "0" }
                val monthsText = state.goalMonthsText.ifBlank { "0" }
                Text(
                    text = "Para alcanzar ${goalCurrency.symbol} $target en $monthsText meses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            KipuTextLink(text = "Saltar meta por ahora", onClick = onSkip)
        }

        KipuCard(style = KipuCardStyle.Large) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = state.hasSocialDebt,
                        role = Role.Switch,
                        onValueChange = onSocialDebtToggled,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿Le debes a alguien?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Deudas sociales cuentan en tu plan mensual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = state.hasSocialDebt,
                    onCheckedChange = null,
                )
            }

            if (state.hasSocialDebt) {
                KipuPenOutlinedTextField(
                    value = state.socialDebtCounterparty,
                    onValueChange = onSocialDebtCounterpartyChanged,
                    label = "¿A quién le debes?",
                    placeholder = "Ej. Juan",
                    showPrefix = false,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    modifier = Modifier.padding(top = 12.dp),
                )
                KipuPenOutlinedTextField(
                    value = state.socialDebtAmountText,
                    onValueChange = onSocialDebtAmountChanged,
                    label = "Monto pendiente",
                    placeholder = "80",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
fun PlanSummaryContent(
    state: PlanWizardUiState.Content,
    onNavigateToStep: (PlanWizardStep) -> Unit,
) {
    val displayBudgets = state.previewBudgets.ifEmpty { state.budgets }

    when (val validation = state.validation) {
        is FinancialPlanValidationResult.Invalid -> {
            KipuAlertCard(tone = KipuAlertTone.Warning, modifier = Modifier.padding(bottom = KipuLayout.sectionSpacing)) {
                Text(text = "Tu plan necesita ajuste", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Tus gastos superan tus ingresos",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        FinancialPlanValidationResult.Valid,
        null,
        -> Unit
    }

    state.cycleAvailable?.cycleAvailable?.let { daily ->
        KipuHeroCard(modifier = Modifier.padding(bottom = KipuLayout.sectionSpacing)) {
            Text(
                text = "PUEDES GASTAR HOY APROXIMADAMENTE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KipuAmountText(amount = daily.amount, type = AmountType.INCOME, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = PlanWizardCycleText.budgetDescription(state.budgetCycle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    KipuCard(style = KipuCardStyle.Large, modifier = Modifier.padding(bottom = KipuLayout.sectionSpacing)) {
        Text(
            text = PlanWizardCycleText.envelopeTitle(state.budgetCycle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        displayBudgets.forEach { budget ->
            EnvelopeSummaryRow(budget, state.budgetCycle)
        }
        if (!state.goalSkipped && state.suggestedGoalWeekly != null) {
            val goalCurrency = state.goalType.currency()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(icon = Icons.Default.Savings, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = "Meta: ${state.goalName}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${goalCurrency.symbol} ${state.suggestedGoalWeekly.amount.stripTrailingZeros().toPlainString()} por semana",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    val income = parseIncomeDisplay(state)
    val fixedTotal = parseFixedDisplay(state)
    if (income != null && fixedTotal != null) {
        KipuCard(style = KipuCardStyle.Large, modifier = Modifier.padding(bottom = KipuLayout.sectionSpacing)) {
            Text(text = "Resumen mensual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SummaryRow("Ingreso estimado", formatPenAmountForDisplay(income))
            SummaryRow("Gastos fijos", "- ${formatPenAmountForDisplay(fixedTotal)}", isExpense = true)
            state.monthlyEnvelopeTotal?.let { envelopes ->
                SummaryRow(
                    "Sobres (equivalente mensual)",
                    "- ${formatPenAmountForDisplay(envelopes.amount)}",
                    isExpense = true,
                )
            }
            state.reserveMonthlyContributionText.toBigDecimalOrNull()
                ?.takeIf { it.signum() > 0 }
                ?.let { reserve ->
                    SummaryRow(
                        "Reserva para imprevistos",
                        "- ${formatPenAmountForDisplay(reserve)}",
                        isExpense = true,
                    )
                }
            state.monthlyExtraAvailable?.let { extra ->
                val isNegative = extra.signum() < 0
                SummaryRow(
                    label = if (isNegative) "Faltante mensual" else "Disponible extra",
                    value = formatPenAmountForDisplay(extra.abs()),
                    isExpense = isNegative,
                )
            }
        }
    }

    if (state.validation is FinancialPlanValidationResult.Invalid) {
        KipuCard(style = KipuCardStyle.Large) {
            Text(text = "Opciones para ajustar tu plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            AdjustmentAction(PlanWizardCycleText.reduceEnvelopesAction(state.budgetCycle)) {
                onNavigateToStep(PlanWizardStep.Envelopes)
            }
            AdjustmentAction("Bajar meta semanal") { onNavigateToStep(PlanWizardStep.Goal) }
            AdjustmentAction("Revisar gastos fijos") { onNavigateToStep(PlanWizardStep.FixedExpenses) }
            AdjustmentAction("Usar ingreso conservador") { onNavigateToStep(PlanWizardStep.Income) }
        }
    }
}

@Composable
private fun EnvelopeSummaryRow(
    budget: EnvelopeBudgetState,
    budgetCycle: BudgetCycle,
) {
    val icon = when {
        budget.name.contains("Comida", ignoreCase = true) -> Icons.Default.Fastfood
        budget.name.contains("Transporte", ignoreCase = true) -> Icons.Default.DirectionsCar
        budget.name.contains("Ocio", ignoreCase = true) -> Icons.Default.SportsEsports
        budget.name.contains("Familia", ignoreCase = true) -> Icons.Default.Groups
        budget.name.contains("hormiga", ignoreCase = true) -> Icons.Default.BugReport
        else -> Icons.Default.Fastfood
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon = icon, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = budget.name, fontWeight = FontWeight.Bold)
            Text(
                text = "${formatPenAmountForDisplay(budget.weeklyLimit.amount)} " +
                    PlanWizardCycleText.envelopeLimitSuffix(budgetCycle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AdjustmentAction(label: String, onClick: () -> Unit) {
    KipuSecondaryButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
        fillWidth = true,
    )
}

@Composable
private fun SummaryRow(label: String, value: String, isExpense: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private val monthlyIncomeEstimator = EstimateMonthlyIncomeUseCase()

internal fun parseIncomeDisplay(state: PlanWizardUiState.Content): BigDecimal? =
    when (val result = monthlyIncomeEstimator.estimateMonthlyIncome(state)) {
        is DomainResult.Ok -> result.value.amount
        is DomainResult.Err -> null
    }

internal fun parseFixedDisplay(state: PlanWizardUiState.Content): BigDecimal? {
    if (state.skipFixedExpenses) return BigDecimal.ZERO
    return when (
        val result = FixedExpenseBreakdownCalculator.sumAll(
            presetParts = listOf(
                state.electricityText,
                state.waterText,
                state.internetText,
                state.rentText,
                state.phoneText,
                state.debtsText,
                state.educationText,
            ),
            customLines = state.customExpenseLines,
        )
    ) {
        is DomainResult.Ok -> result.value.amount
        is DomainResult.Err -> null
    }
}


@Composable
private fun WizardLineItemRow(
    label: String,
    amountText: String,
    labelPlaceholder: String,
    onLabelChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KipuPenOutlinedTextField(
                value = label,
                onValueChange = onLabelChanged,
                label = labelPlaceholder,
                showPrefix = false,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                modifier = Modifier.weight(1f),
            )
            KipuPenOutlinedTextField(
                value = amountText,
                onValueChange = onAmountChanged,
                label = "Monto",
                modifier = Modifier.weight(0.8f),
            )
        }
        KipuTextLink(
            text = "Quitar",
            onClick = onRemove,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
