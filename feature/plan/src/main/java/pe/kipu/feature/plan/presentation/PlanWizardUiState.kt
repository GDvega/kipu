package pe.kipu.feature.plan.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.CycleAvailableBudget
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.GoalType
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.plan.PlanWizardLineItem
import java.math.BigDecimal

sealed interface PlanWizardUiState {
    data object Loading : PlanWizardUiState

    data class Error(val message: String) : PlanWizardUiState

    data class Content(
        val step: PlanWizardStep,
        val startStep: PlanWizardStep = PlanWizardStep.Income,
        val incomeProfile: IncomeProfile = IncomeProfile.FIXED,
        val fixedBaseText: String = "",
        val secondQuincenaText: String = "",
        val initialBalanceText: String = "",
        val reserveMonthlyContributionText: String = "",
        val payFrequency: PayFrequency = PayFrequency.MONTHLY,
        val extraIncomeText: String = "",
        val additionalIncomeLines: List<PlanWizardLineItem> = emptyList(),
        val lowWeekText: String = "",
        val normalWeekText: String = "",
        val goodWeekText: String = "",
        val approximateIncomeText: String = "",
        val electricityText: String = "",
        val waterText: String = "",
        val internetText: String = "",
        val rentText: String = "",
        val phoneText: String = "",
        val debtsText: String = "",
        val educationText: String = "",
        val customExpenseLines: List<PlanWizardLineItem> = emptyList(),
        val skipFixedExpenses: Boolean = false,

        val categories: List<Category> = emptyList(),
        val envelopeLimits: Map<String, String> = emptyMap(),
        val customizingEnvelopeId: String? = null,
        val antSpendingLimitText: String = "",
        /** Selected category ids for ant-spending tracking (Room). */
        val antSpendingCategories: Set<String> = emptySet(),
        val antSpendingAlertEnabled: Boolean = true,
        val goalSkipped: Boolean = false,
        val goalType: GoalType = GoalType.EMERGENCY,
        val goalName: String = "",
        val goalTargetText: String = "",
        val goalCurrentText: String = "",
        val goalMonthsText: String = "5",
        val suggestedGoalWeekly: Money? = null,
        val hasSocialDebt: Boolean = false,
        val socialDebtCounterparty: String = "",
        val socialDebtAmountText: String = "",
        val budgets: List<EnvelopeBudgetState> = emptyList(),
        val previewBudgets: List<EnvelopeBudgetState> = emptyList(),
        val cycleAvailable: CycleAvailableBudget? = null,
        val validation: FinancialPlanValidationResult? = null,
        val monthlyEnvelopeTotal: Money? = null,
        val monthlyExtraAvailable: BigDecimal? = null,
        val isSaving: Boolean = false,
        val isEditingExistingPlan: Boolean = false,
        val errorMessage: String? = null,
        val pendingAntCategoryName: String = "",
        val customEnvelopeLines: List<PlanWizardLineItem> = emptyList(),
        val budgetCycle: pe.kipu.core.domain.model.BudgetCycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
    ) : PlanWizardUiState
}
