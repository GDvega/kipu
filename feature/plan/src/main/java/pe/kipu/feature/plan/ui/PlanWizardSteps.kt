package pe.kipu.feature.plan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.FixedExpenseBreakdownCalculator
import pe.kipu.core.domain.plan.GoalTimeframeOptions
import pe.kipu.core.domain.plan.GoalType
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.plan.PlanEnvelopeTemplates
import pe.kipu.core.domain.plan.currency
import pe.kipu.core.domain.plan.label
import pe.kipu.core.domain.plan.subtitle
import pe.kipu.core.domain.plan.title
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.feature.plan.presentation.PlanWizardStep
import pe.kipu.feature.plan.presentation.PlanWizardUiState
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun IncomeStepContent(
    state: PlanWizardUiState.Content,
    onProfileSelected: (IncomeProfile) -> Unit,
    onFixedBaseChanged: (String) -> Unit,
    onPayFrequencySelected: (PayFrequency) -> Unit,
    onExtraIncomeChanged: (String) -> Unit,
    onLowWeekChanged: (String) -> Unit,
    onNormalWeekChanged: (String) -> Unit,
    onGoodWeekChanged: (String) -> Unit,
    onApproximateIncomeChanged: (String) -> Unit,
    onSkipApproximate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IncomeProfile.entries.forEach { profile ->
            KipuSelectionCard(
                title = profile.title(),
                subtitle = profile.subtitle(),
                selected = state.incomeProfile == profile,
                onClick = { onProfileSelected(profile) },
                leading = { IncomeProfileIcon(profile) },
            )
        }

        when (state.incomeProfile) {
            IncomeProfile.FIXED -> {
                KipuPenOutlinedTextField(
                    value = state.fixedBaseText,
                    onValueChange = onFixedBaseChanged,
                    label = "Sueldo mensual (aproximado)",
                    placeholder = "1500",
                )
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
                KipuPenOutlinedTextField(
                    value = state.extraIncomeText,
                    onValueChange = onExtraIncomeChanged,
                    label = "Yapeos y extras del mes (opcional)",
                    placeholder = "300",
                )
            }

            IncomeProfile.VARIABLE -> {
                KipuPenOutlinedTextField(
                    value = state.lowWeekText,
                    onValueChange = onLowWeekChanged,
                    label = "Semana baja",
                    placeholder = "250",
                )
                KipuPenOutlinedTextField(
                    value = state.normalWeekText,
                    onValueChange = onNormalWeekChanged,
                    label = "Semana normal",
                    placeholder = "400",
                )
                KipuPenOutlinedTextField(
                    value = state.goodWeekText,
                    onValueChange = onGoodWeekChanged,
                    label = "Semana buena",
                    placeholder = "650",
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
                    KipuTextLink(text = "Saltar por ahora", onClick = onSkipApproximate)
                }
            }
        }
    }
}

@Composable
private fun IncomeProfileIcon(profile: IncomeProfile) {
    val (icon, tint) = when (profile) {
        IncomeProfile.FIXED -> Icons.Default.Work to MaterialTheme.colorScheme.primary
        IncomeProfile.VARIABLE -> Icons.AutoMirrored.Filled.TrendingUp to MaterialTheme.colorScheme.tertiary
        IncomeProfile.APPROXIMATE -> Icons.Default.HelpOutline to MaterialTheme.colorScheme.secondary
    }
    IconBadge(icon = icon, tint = tint)
}

@Composable
fun FixedExpensesStepContent(
    educationText: String,
    rentText: String,
    utilitiesText: String,
    phoneText: String,
    debtsText: String,
    onEducationChanged: (String) -> Unit,
    onRentChanged: (String) -> Unit,
    onUtilitiesChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onDebtsChanged: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val totalText = FixedExpenseBreakdownCalculator.formatTotal(
        educationText, rentText, utilitiesText, phoneText, debtsText,
    )

    KipuCard(style = KipuCardStyle.Large) {
        Text(
            text = "Gastos fijos principales",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FixedExpenseRow(Icons.Default.School, "Universidad / instituto", educationText, onEducationChanged)
        FixedExpenseRow(Icons.Default.Home, "Alquiler / casa", rentText, onRentChanged)
        FixedExpenseRow(Icons.Default.Thunderstorm, "Luz, agua, internet", utilitiesText, onUtilitiesChanged)
        FixedExpenseRow(Icons.Default.PhoneAndroid, "Celular", phoneText, onPhoneChanged)
        FixedExpenseRow(Icons.Default.Handshake, "Préstamo / deuda", debtsText, onDebtsChanged)
    }

    KipuCard(
        modifier = Modifier.padding(top = KipuLayout.sectionSpacing),
        style = KipuCardStyle.Large,
    ) {
        Text(text = "Total gastos fijos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = if (totalText.isNotBlank()) "S/ $totalText" else "S/ 0",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Se restarán de tu ingreso mensual",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
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
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon = icon, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        KipuPenOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun EnvelopesStepContent(
    envelopeLimits: Map<String, String>,
    customizingEnvelopeId: String?,
    onPresetSelected: (String, BigDecimal) -> Unit,
    onLimitChanged: (String, String) -> Unit,
    onCustomize: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                KipuAmountPresetRow(
                    presets = template.presetAmounts,
                    selectedAmount = selectedAmount,
                    onPresetSelected = { onPresetSelected(template.envelopeId, it) },
                    onCustomize = { onCustomize(template.envelopeId) },
                )
                if (customizingEnvelopeId == template.envelopeId) {
                    KipuPenOutlinedTextField(
                        value = limitText,
                        onValueChange = { onLimitChanged(template.envelopeId, it) },
                        label = "Monto personalizado por semana",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AntSpendingStepContent(
    limitText: String,
    selectedCategories: Set<String>,
    alertEnabled: Boolean,
    onLimitChanged: (String) -> Unit,
    onPresetSelected: (BigDecimal) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onAlertToggled: (Boolean) -> Unit,
) {
    val categories = listOf(
        "Agua / gaseosa",
        "Snacks",
        "Cafecito",
        "Menú",
        "Taxi corto",
        "Copias",
        "Recargas",
        "Bodega",
    )
    val limitAmount = limitText.toBigDecimalOrNull() ?: BigDecimal("35")
    val alertAt80 = limitAmount.multiply(BigDecimal("0.8")).setScale(0, RoundingMode.HALF_UP)

    KipuCard(style = KipuCardStyle.Large) {
        Text(
            text = "¿Qué gastos pequeños se te escapan más?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                KipuFilterChip(
                    text = category,
                    selected = category in selectedCategories,
                    onClick = { onCategoryToggled(category) },
                )
            }
        }
    }

    KipuCard(modifier = Modifier.padding(top = KipuLayout.sectionSpacing), style = KipuCardStyle.Large) {
        Text(text = "Límite semanal de gastos hormiga", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "S/ ${limitAmount.stripTrailingZeros().toPlainString()}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Slider(
            value = limitAmount.toFloat().coerceIn(10f, 100f),
            onValueChange = { onLimitChanged(it.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toPlainString()) },
            valueRange = 10f..100f,
            steps = 18,
        )
        KipuAmountPresetRow(
            presets = PlanEnvelopeTemplates.ANT_SPENDING_PRESETS,
            selectedAmount = limitAmount,
            onPresetSelected = onPresetSelected,
        )
        KipuAlertCard(tone = KipuAlertTone.Info, modifier = Modifier.padding(top = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Switch(checked = alertEnabled, onCheckedChange = onAlertToggled)
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

@Composable
fun GoalStepContent(
    state: PlanWizardUiState.Content,
    onGoalTypeSelected: (GoalType) -> Unit,
    onGoalNameChanged: (String) -> Unit,
    onGoalTargetChanged: (String) -> Unit,
    onGoalCurrentChanged: (String) -> Unit,
    onGoalMonthsSelected: (Int) -> Unit,
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
        KipuFilterChipRow(
            labels = GoalTimeframeOptions.MONTHS.map { GoalTimeframeOptions.label(it) },
            selectedIndex = GoalTimeframeOptions.MONTHS.indexOf(state.goalMonths).coerceAtLeast(0),
            onSelected = { index -> onGoalMonthsSelected(GoalTimeframeOptions.MONTHS[index]) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
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
                Text(
                    text = "Para alcanzar ${goalCurrency.symbol} $target en ${GoalTimeframeOptions.label(state.goalMonths)}",
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
                modifier = Modifier.fillMaxWidth(),
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
                    onCheckedChange = onSocialDebtToggled,
                )
            }

            if (state.hasSocialDebt) {
                KipuPenOutlinedTextField(
                    value = state.socialDebtCounterparty,
                    onValueChange = onSocialDebtCounterpartyChanged,
                    label = "¿A quién le debes?",
                    placeholder = "Ej. Juan",
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

    state.dailyAvailable?.dailyAvailable?.let { daily ->
        KipuHeroCard(modifier = Modifier.padding(bottom = KipuLayout.sectionSpacing)) {
            Text(
                text = "PUEDES GASTAR HOY APROXIMADAMENTE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KipuAmountText(amount = daily.amount, type = AmountType.INCOME, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Sin descuadrarte de tu presupuesto semanal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    KipuCard(style = KipuCardStyle.Large, modifier = Modifier.padding(bottom = KipuLayout.sectionSpacing)) {
        Text(text = "Tus sobres semanales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        displayBudgets.forEach { budget ->
            EnvelopeSummaryRow(budget)
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
                SummaryRow("Sobres semanales", "- ${formatPenAmountForDisplay(envelopes.amount)}", isExpense = true)
            }
            state.monthlyExtraAvailable?.let { extra ->
                val isNegative = extra.amount.signum() < 0
                SummaryRow(
                    label = "Disponible extra",
                    value = formatPenAmountForDisplay(extra.amount),
                    isExpense = isNegative,
                )
            }
        }
    }

    if (state.validation is FinancialPlanValidationResult.Invalid) {
        KipuCard(style = KipuCardStyle.Large) {
            Text(text = "Opciones para ajustar tu plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            AdjustmentAction("Reducir sobres semanales") { onNavigateToStep(PlanWizardStep.Envelopes) }
            AdjustmentAction("Bajar meta semanal") { onNavigateToStep(PlanWizardStep.Goal) }
            AdjustmentAction("Revisar gastos fijos") { onNavigateToStep(PlanWizardStep.FixedExpenses) }
            AdjustmentAction("Usar ingreso conservador") { onNavigateToStep(PlanWizardStep.Income) }
        }
    }
}

@Composable
private fun EnvelopeSummaryRow(budget: EnvelopeBudgetState) {
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
                text = "${formatPenAmountForDisplay(budget.weeklyLimit.amount)} por semana",
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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

private fun parseIncomeDisplay(state: PlanWizardUiState.Content): BigDecimal? {
    val text = when (state.incomeProfile) {
        IncomeProfile.FIXED -> state.fixedBaseText
        IncomeProfile.VARIABLE -> state.normalWeekText
        IncomeProfile.APPROXIMATE -> state.approximateIncomeText
    }
    return when (val parsed = MoneyInputParser.parsePen(text)) {
        is DomainResult.Ok -> parsed.value.amount
        is DomainResult.Err -> null
    }
}

private fun parseFixedDisplay(state: PlanWizardUiState.Content): BigDecimal? {
    if (state.skipFixedExpenses) return BigDecimal.ZERO
    return when (
        val result = FixedExpenseBreakdownCalculator.sumParts(
            state.educationText,
            state.rentText,
            state.utilitiesText,
            state.phoneText,
            state.debtsText,
        )
    ) {
        is DomainResult.Ok -> result.value.amount
        is DomainResult.Err -> null
    }
}
