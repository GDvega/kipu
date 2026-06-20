package pe.kipu.feature.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
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
    modifier: Modifier = Modifier,
    viewModel: PlanWizardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        PlanWizardUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KipuLoadingIndicator()
            }
        }

        is PlanWizardUiState.Content -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = KipuLayout.screenHorizontalPadding),
            ) {
                KipuScreenHeader(
                    title = wizardTitle(state.step),
                    subtitle = wizardSubtitle(state.step),
                )

                KipuWizardProgressDots(
                    currentStep = state.step.stepIndex(),
                    totalSteps = PLAN_WIZARD_TOTAL_STEPS,
                    modifier = Modifier
                        .padding(
                            horizontal = KipuLayout.screenHorizontalPadding,
                            vertical = KipuLayout.sectionSpacing,
                        )
                        .fillMaxWidth(),
                )

                Column(modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding)) {
                    when (state.step) {
                        PlanWizardStep.Income -> IncomeStepContent(
                            state = state,
                            onProfileSelected = viewModel::onIncomeProfileSelected,
                            onFixedBaseChanged = viewModel::onFixedBaseChanged,
                            onPayFrequencySelected = viewModel::onPayFrequencySelected,
                            onExtraIncomeChanged = viewModel::onExtraIncomeChanged,
                            onLowWeekChanged = viewModel::onLowWeekChanged,
                            onNormalWeekChanged = viewModel::onNormalWeekChanged,
                            onGoodWeekChanged = viewModel::onGoodWeekChanged,
                            onApproximateIncomeChanged = viewModel::onApproximateIncomeChanged,
                            onSkipApproximate = viewModel::onSkipApproximate,
                        )

                        PlanWizardStep.FixedExpenses -> FixedExpensesStepContent(
                            educationText = state.educationText,
                            rentText = state.rentText,
                            utilitiesText = state.utilitiesText,
                            phoneText = state.phoneText,
                            debtsText = state.debtsText,
                            onEducationChanged = viewModel::onEducationChanged,
                            onRentChanged = viewModel::onRentChanged,
                            onUtilitiesChanged = viewModel::onUtilitiesChanged,
                            onPhoneChanged = viewModel::onPhoneChanged,
                            onDebtsChanged = viewModel::onDebtsChanged,
                            onSkip = viewModel::onSkipFixedExpenses,
                        )

                        PlanWizardStep.Envelopes -> EnvelopesStepContent(
                            envelopeLimits = state.envelopeLimits,
                            customizingEnvelopeId = state.customizingEnvelopeId,
                            onPresetSelected = viewModel::onEnvelopePresetSelected,
                            onLimitChanged = viewModel::onEnvelopeLimitChanged,
                            onCustomize = viewModel::onCustomizeEnvelope,
                        )

                        PlanWizardStep.AntSpending -> AntSpendingStepContent(
                            limitText = state.antSpendingLimitText,
                            selectedCategories = state.antSpendingCategories,
                            alertEnabled = state.antSpendingAlertEnabled,
                            onLimitChanged = viewModel::onAntSpendingLimitChanged,
                            onPresetSelected = viewModel::onAntSpendingPresetSelected,
                            onCategoryToggled = viewModel::onAntCategoryToggled,
                            onAlertToggled = viewModel::onAntSpendingAlertToggled,
                        )

                        PlanWizardStep.Goal -> GoalStepContent(
                            state = state,
                            onGoalTypeSelected = viewModel::onGoalTypeSelected,
                            onGoalNameChanged = viewModel::onGoalNameChanged,
                            onGoalTargetChanged = viewModel::onGoalTargetChanged,
                            onGoalCurrentChanged = viewModel::onGoalCurrentChanged,
                            onGoalMonthsSelected = viewModel::onGoalMonthsSelected,
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

                    if (state.step != PlanWizardStep.Income && state.step != PlanWizardStep.Summary) {
                        KipuSecondaryButton(
                            text = "Atrás",
                            onClick = viewModel::onBack,
                            modifier = Modifier.padding(top = 24.dp),
                            fillWidth = true,
                        )
                    }

                    if (state.step != PlanWizardStep.Summary) {
                        KipuPrimaryButton(
                            text = "Continuar →",
                            onClick = viewModel::onContinue,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    } else {
                        KipuPrimaryButton(
                            text = if (state.isSaving) "Guardando..." else "✓ Crear mi plan",
                            onClick = { viewModel.onFinish(onFinished) },
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

private fun wizardTitle(step: PlanWizardStep): String = when (step) {
    PlanWizardStep.Income -> "¿Cuánto dinero recibes?"
    PlanWizardStep.FixedExpenses -> "¿Qué pagos sí o sí tienes?"
    PlanWizardStep.Envelopes -> "¿Cuánto quieres gastar a la semana?"
    PlanWizardStep.AntSpending -> "Gastos hormiga"
    PlanWizardStep.Goal -> "¿Qué quieres lograr?"
    PlanWizardStep.Summary -> "Tu plan está listo"
}

private fun wizardSubtitle(step: PlanWizardStep): String = when (step) {
    PlanWizardStep.Income -> "Selecciona la opción que mejor describa tus ingresos."
    PlanWizardStep.FixedExpenses -> "Estos son los gastos que no puedes evitar cada mes."
    PlanWizardStep.Envelopes -> "Estos son los sobres que Kipu creará para ti. Elige rápido o personaliza."
    PlanWizardStep.AntSpending -> "Esos pequeños pagos que se escapan: agua, gaseosa, snacks, cafecito..."
    PlanWizardStep.Goal -> "Una meta te ayuda a separar plata antes de gastarla."
    PlanWizardStep.Summary -> "Kipu armó tus sobres semanales. Puedes ajustarlos cuando quieras."
}
