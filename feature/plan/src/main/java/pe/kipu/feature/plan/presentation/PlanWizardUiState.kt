package pe.kipu.feature.plan.presentation

import pe.kipu.core.domain.model.DailyAvailableBudget
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.GoalType
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency

sealed interface PlanWizardUiState {
    data object Loading : PlanWizardUiState

    data class Content(
        val step: PlanWizardStep,
        val incomeProfile: IncomeProfile = IncomeProfile.FIXED,
        val fixedBaseText: String = "",
        val payFrequency: PayFrequency = PayFrequency.MONTHLY,
        val extraIncomeText: String = "",
        val lowWeekText: String = "",
        val normalWeekText: String = "",
        val goodWeekText: String = "",
        val approximateIncomeText: String = "",
        val educationText: String = "",
        val rentText: String = "",
        val utilitiesText: String = "",
        val phoneText: String = "",
        val debtsText: String = "",
        val skipFixedExpenses: Boolean = false,
        val envelopeLimits: Map<String, String> = emptyMap(),
        val customizingEnvelopeId: String? = null,
        val antSpendingLimitText: String = "",
        val antSpendingCategories: Set<String> = emptySet(),
        val antSpendingAlertEnabled: Boolean = true,
        val goalSkipped: Boolean = false,
        val goalType: GoalType = GoalType.EMERGENCY,
        val goalName: String = "",
        val goalTargetText: String = "",
        val goalCurrentText: String = "",
        val goalMonths: Int = 5,
        val suggestedGoalWeekly: Money? = null,
        val hasSocialDebt: Boolean = false,
        val socialDebtCounterparty: String = "",
        val socialDebtAmountText: String = "",
        val budgets: List<EnvelopeBudgetState> = emptyList(),
        val previewBudgets: List<EnvelopeBudgetState> = emptyList(),
        val dailyAvailable: DailyAvailableBudget? = null,
        val validation: FinancialPlanValidationResult? = null,
        val monthlyEnvelopeTotal: Money? = null,
        val monthlyExtraAvailable: Money? = null,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
    ) : PlanWizardUiState
}
