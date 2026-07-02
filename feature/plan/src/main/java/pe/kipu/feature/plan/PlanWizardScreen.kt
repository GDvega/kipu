package pe.kipu.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSubScreenScaffold
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuWizardProgressDots
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.feature.plan.presentation.PLAN_WIZARD_TOTAL_STEPS
import pe.kipu.feature.plan.presentation.PlanWizardStep
import pe.kipu.feature.plan.presentation.PlanWizardUiState
import pe.kipu.feature.plan.presentation.PlanWizardViewModel
import pe.kipu.feature.plan.presentation.stepIndex
import pe.kipu.feature.plan.ui.AntSpendingStepContent
import pe.kipu.feature.plan.ui.EnvelopesStepContent
import pe.kipu.feature.plan.ui.FixedExpensesStepContent
import pe.kipu.feature.plan.ui.GoalStepContent
import pe.kipu.feature.plan.ui.IncomeStepContent
import pe.kipu.feature.plan.ui.PlanSummaryContent

@Composable
fun PlanWizardScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit = onFinished,
    onCancelNewPlan: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: PlanWizardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    when (val state = uiState) {
        PlanWizardUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KipuLoadingIndicator()
            }
        }

        is PlanWizardUiState.Error -> {
            KipuErrorState(
                title = "No pudimos cargar tu plan",
                message = state.message,
                retryLabel = "Reintentar",
                onRetry = viewModel::retryLoad,
                modifier = modifier.fillMaxSize(),
            )
        }

        is PlanWizardUiState.Content -> {
            val handleBack = {
                if (state.step == PlanWizardStep.Income) {
                    if (state.isEditingExistingPlan) {
                        onBack()
                    } else {
                        onCancelNewPlan()
                    }
                } else {
                    viewModel.onBack()
                }
            }

            KipuSubScreenScaffold(
                title = wizardTitle(state.step),
                onBack = handleBack,
                modifier = modifier,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = wizardSubtitle(state.step),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = KipuLayout.screenHorizontalPadding,
                            vertical = 8.dp,
                        ),
                    )

                    KipuWizardProgressDots(
                        currentStep = state.step.stepIndex(),
                        totalSteps = PLAN_WIZARD_TOTAL_STEPS,
                        modifier = Modifier
                            .padding(horizontal = KipuLayout.screenHorizontalPadding)
                            .padding(
                                top = KipuLayout.sectionSpacing,
                                bottom = if (state.step != PlanWizardStep.Income && state.step != PlanWizardStep.Summary) 8.dp else KipuLayout.sectionSpacing,
                            )
                            .fillMaxWidth(),
                    )

                    if (state.step != PlanWizardStep.Income && state.step != PlanWizardStep.Summary) {
                        WizardBalanceStickyBanner(
                            state = state,
                            modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding).padding(bottom = KipuLayout.sectionSpacing)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = KipuLayout.screenHorizontalPadding)
                    ) {
                        AnimatedContent(
                            targetState = state.step,
                            label = "wizard_step_transition",
                            modifier = Modifier.fillMaxWidth(),
                            transitionSpec = {
                                if (targetState.stepIndex() > initialState.stepIndex()) {
                                    slideInHorizontally(
                                        animationSpec = tween(300),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    ) togetherWith slideOutHorizontally(
                                        animationSpec = tween(300),
                                        targetOffsetX = { fullWidth -> -fullWidth }
                                    )
                                } else {
                                    slideInHorizontally(
                                        animationSpec = tween(300),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    ) togetherWith slideOutHorizontally(
                                        animationSpec = tween(300),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            }
                        ) { targetStep ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp) // Espacio para que no choque con los botones inferiores
                            ) {
                                when (targetStep) {
                                    PlanWizardStep.Income -> IncomeStepContent(
                                        state = state,
                                        onProfileSelected = viewModel::onIncomeProfileSelected,
                                        onFixedBaseChanged = viewModel::onFixedBaseChanged,
                                        onInitialBalanceChanged = viewModel::onInitialBalanceChanged,
                                        onSecondQuincenaChanged = viewModel::onSecondQuincenaChanged,
                                        onPayFrequencySelected = viewModel::onPayFrequencySelected,
                                        onLowWeekChanged = viewModel::onLowWeekChanged,
                                        onNormalWeekChanged = viewModel::onNormalWeekChanged,
                                        onGoodWeekChanged = viewModel::onGoodWeekChanged,
                                        onApproximateIncomeChanged = viewModel::onApproximateIncomeChanged,
                                        onAddIncomeLine = viewModel::onAddIncomeLine,
                                        onRemoveIncomeLine = viewModel::onRemoveIncomeLine,
                                        onIncomeLineChanged = viewModel::onIncomeLineChanged,
                                        onSkipApproximate = viewModel::onSkipApproximate,
                                    )

                                    PlanWizardStep.FixedExpenses -> FixedExpensesStepContent(
                                        educationText = state.educationText,
                                        rentText = state.rentText,
                                        utilitiesText = state.utilitiesText,
                                        phoneText = state.phoneText,
                                        debtsText = state.debtsText,
                                        customExpenseLines = state.customExpenseLines,
                                        onEducationChanged = viewModel::onEducationChanged,
                                        onRentChanged = viewModel::onRentChanged,
                                        onUtilitiesChanged = viewModel::onUtilitiesChanged,
                                        onPhoneChanged = viewModel::onPhoneChanged,
                                        onDebtsChanged = viewModel::onDebtsChanged,
                                        onCustomLineChanged = viewModel::onCustomExpenseLineChanged,
                                        onAddCustomExpenseLine = viewModel::onAddCustomExpenseLine,
                                        onRemoveCustomExpenseLine = viewModel::onRemoveCustomExpenseLine,
                                        onQuickExpenseSelected = viewModel::onQuickExpenseSelected,
                                        onSkip = viewModel::onSkipFixedExpenses,
                                    )

                                    PlanWizardStep.Envelopes -> EnvelopesStepContent(
                                        budgetCycle = state.budgetCycle,
                                        envelopeLimits = state.envelopeLimits,
                                        customizingEnvelopeId = state.customizingEnvelopeId,
                                        customEnvelopeLines = state.customEnvelopeLines,
                                        onBudgetCycleSelected = viewModel::onBudgetCycleSelected,
                                        onPresetSelected = viewModel::onEnvelopePresetSelected,
                                        onLimitChanged = viewModel::onEnvelopeLimitChanged,
                                        onCustomize = viewModel::onCustomizeEnvelope,
                                        onAddCustomEnvelope = viewModel::onAddCustomEnvelopeLine,
                                        onRemoveCustomEnvelope = viewModel::onRemoveCustomEnvelopeLine,
                                        onCustomEnvelopeChanged = viewModel::onCustomEnvelopeLineChanged,
                                    )

                                    PlanWizardStep.AntSpending -> AntSpendingStepContent(
                                        categories = state.categories,
                                        limitText = state.antSpendingLimitText,
                                        selectedCategoryIds = state.antSpendingCategories,
                                        pendingCategoryName = state.pendingAntCategoryName,
                                        alertEnabled = state.antSpendingAlertEnabled,
                                        onLimitChanged = viewModel::onAntSpendingLimitChanged,
                                        onPresetSelected = viewModel::onAntSpendingPresetSelected,
                                        onCategoryToggled = viewModel::onAntCategoryToggled,
                                        onPendingCategoryNameChanged = viewModel::onPendingAntCategoryNameChanged,
                                        onAddAntCategory = viewModel::onAddAntCategory,
                                        onQuickAntCategorySelected = viewModel::onQuickAntCategorySelected,
                                        onAlertToggled = viewModel::onAntSpendingAlertToggled,
                                    )

                                    PlanWizardStep.Goal -> GoalStepContent(
                                        state = state,
                                        onGoalTypeSelected = viewModel::onGoalTypeSelected,
                                        onGoalNameChanged = viewModel::onGoalNameChanged,
                                        onGoalTargetChanged = viewModel::onGoalTargetChanged,
                                        onGoalCurrentChanged = viewModel::onGoalCurrentChanged,
                                        onGoalMonthsChanged = viewModel::onGoalMonthsChanged,
                                        onSocialDebtToggled = viewModel::onSocialDebtToggled,
                                        onSocialDebtCounterpartyChanged = viewModel::onSocialDebtCounterpartyChanged,
                                        onSocialDebtAmountChanged = viewModel::onSocialDebtAmountChanged,
                                        onSkip = viewModel::onSkipGoal,
                                    )

                                    PlanWizardStep.Summary -> PlanSummaryContent(
                                        state = state,
                                        onNavigateToStep = viewModel::onNavigateToStep,
                                    )
                                }

                                state.errorMessage?.let { message ->
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 12.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KipuLayout.screenHorizontalPadding)
                            .padding(bottom = KipuLayout.screenHorizontalPadding)
                    ) {
                        if (state.step != PlanWizardStep.Income && state.step != PlanWizardStep.Summary) {
                            KipuSecondaryButton(
                                text = "Atrás",
                                onClick = handleBack,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (state.step != PlanWizardStep.Summary) {
                            KipuPrimaryButton(
                                text = "Continuar →",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onContinue()
                                },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        } else {
                            KipuPrimaryButton(
                                text = when {
                                    state.isSaving -> "Guardando..."
                                    state.isEditingExistingPlan -> "✓ Guardar mi plan"
                                    else -> "✓ Crear mi plan"
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onFinish(onFinished)
                                },
                                enabled = !state.isSaving && state.validation !is FinancialPlanValidationResult.Invalid,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                            if (state.validation is FinancialPlanValidationResult.Invalid) {
                                Text(
                                    text = "Ajusta tu plan para que tus ingresos cubran gastos y sobres antes de guardar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            KipuSecondaryButton(
                                text = "Ajustar montos",
                                onClick = { viewModel.onNavigateToStep(PlanWizardStep.Income) },
                                modifier = Modifier.padding(top = 12.dp),
                                fillWidth = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardBalanceStickyBanner(
    state: PlanWizardUiState.Content,
    modifier: Modifier = Modifier
) {
    val income = pe.kipu.feature.plan.ui.parseIncomeDisplay(state) ?: java.math.BigDecimal.ZERO
    val free = state.monthlyExtraAvailable ?: income
    val assigned = income - free

    val isNegative = free.signum() < 0
    val isWarning = !isNegative && free <= income.multiply(java.math.BigDecimal("0.15"))
    val freeColor = when {
        isNegative -> MaterialTheme.colorScheme.error
        isWarning -> pe.kipu.core.designsystem.theme.KipuAmber
        else -> MaterialTheme.colorScheme.primary
    }

    // Progress fraction: how much of income is still free (0f-1f)
    val fraction = if (income.signum() > 0) {
        (free.toFloat() / income.toFloat()).coerceIn(0f, 1f)
    } else 1f

    pe.kipu.core.designsystem.component.KipuCard(
        modifier = modifier.fillMaxWidth(),
        style = pe.kipu.core.designsystem.component.KipuCardStyle.Default
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Bottom
            ) {
                // Ingreso
                Column {
                    Text(
                        "Ingreso mes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        pe.kipu.core.designsystem.component.formatPenAmountForDisplay(income),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
                // Asignado — centered
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        "Asignado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        pe.kipu.core.designsystem.component.formatPenAmountForDisplay(assigned),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
                // Libre — right, colored
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        "Libre",
                        style = MaterialTheme.typography.labelSmall,
                        color = freeColor,
                    )
                    Text(
                        pe.kipu.core.designsystem.component.formatPenAmountForDisplay(free),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = freeColor,
                    )
                }
            }
            // Progress bar: shows how much remains free
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                        .background(freeColor)
                )
            }
        }
    }
}

private fun wizardTitle(step: PlanWizardStep): String = when (step) {
    PlanWizardStep.Income -> "¿Cuánto dinero recibes?"
    PlanWizardStep.FixedExpenses -> "¿Qué pagos sí o sí tienes?"
    PlanWizardStep.Envelopes -> "¿Cuánto quieres asignar a tus sobres?"
    PlanWizardStep.AntSpending -> "Gastos hormiga"
    PlanWizardStep.Goal -> "¿Qué quieres lograr?"
    PlanWizardStep.Summary -> "Tu plan está listo"
}

private fun wizardSubtitle(step: PlanWizardStep): String = when (step) {
    PlanWizardStep.Income -> "No te preocupes si no es exacto. Kipu se adapta a lo que tú le digas."
    PlanWizardStep.FixedExpenses -> "¿Cuánto se te va en lo fijo? Si no sabes el monto exacto, pon un aproximado."
    PlanWizardStep.Envelopes -> "¿En qué gastas tu plata del día a día? Ponle un tope a cada cosa."
    PlanWizardStep.AntSpending -> "El cafecito, la gaseosa, el snack... ¿cuánto se te escapa por semana?"
    PlanWizardStep.Goal -> "¿Estás juntando para algo? Kipu te dice cuánto guardar por semana."
    PlanWizardStep.Summary -> "¡Listo! Así queda tu plan. Puedes cambiarlo cuando quieras."
}
