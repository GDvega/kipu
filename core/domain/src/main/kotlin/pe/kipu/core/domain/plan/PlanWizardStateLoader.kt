package pe.kipu.core.domain.plan

import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money

/**
 * Maps persisted plan data into wizard form defaults when re-editing.
 */
object PlanWizardStateLoader {

    data class IncomeDefaults(
        val approximateIncomeText: String,
        val fixedBaseText: String,
        val initialBalanceText: String,
    )

    data class FixedExpenseDefaults(
        val skipFixedExpenses: Boolean,
        val educationText: String,
        val rentText: String,
        val utilitiesText: String,
        val phoneText: String,
        val debtsText: String,
    )

    data class GoalDefaults(
        val goalName: String,
        val goalTargetText: String,
        val goalCurrentText: String,
        val goalSkipped: Boolean,
    )

    data class IncomeProfileDefaults(
        val incomeProfile: IncomeProfile,
        val payFrequency: PayFrequency,
    )

    fun incomeProfileDefaults(plan: FinancialPlan?): IncomeProfileDefaults = IncomeProfileDefaults(
        incomeProfile = plan?.incomeProfile ?: IncomeProfile.FIXED,
        payFrequency = plan?.payFrequency ?: PayFrequency.MONTHLY,
    )

    fun incomeDefaults(plan: FinancialPlan?): IncomeDefaults {
        val incomeText = plan?.estimatedMonthlyIncome?.amount?.stripTrailingZeros()?.toPlainString()
            ?: ""
        val balanceText = plan?.initialBalance?.amount?.stripTrailingZeros()?.toPlainString() ?: ""
        return IncomeDefaults(
            approximateIncomeText = incomeText,
            fixedBaseText = incomeText,
            initialBalanceText = balanceText,
        )
    }

    fun fixedExpenseDefaults(plan: FinancialPlan?): FixedExpenseDefaults {
        val fixed = plan?.fixedExpenses
        if (fixed == null) {
            return FixedExpenseDefaults(
                skipFixedExpenses = false,
                educationText = "",
                rentText = "",
                utilitiesText = "",
                phoneText = "",
                debtsText = "",
            )
        }

        if (fixed.isZero()) {
            return FixedExpenseDefaults(
                skipFixedExpenses = true,
                educationText = "",
                rentText = "",
                utilitiesText = "",
                phoneText = "",
                debtsText = "",
            )
        }

        return FixedExpenseDefaults(
            skipFixedExpenses = false,
            educationText = "",
            rentText = "",
            utilitiesText = "",
            phoneText = "",
            debtsText = fixed.amount.stripTrailingZeros().toPlainString(),
        )
    }

    fun goalDefaults(emergencyGoal: Commitment?): GoalDefaults {
        if (emergencyGoal == null || emergencyGoal.type != CommitmentType.SAVINGS_GOAL) {
            return GoalDefaults(
                goalName = GoalType.EMERGENCY.defaultTitle(),
                goalTargetText = "",
                goalCurrentText = "",
                goalSkipped = false,
            )
        }

        return GoalDefaults(
            goalName = emergencyGoal.title,
            goalTargetText = emergencyGoal.targetAmount?.amount?.stripTrailingZeros()?.toPlainString()
                ?: PeruPlanDefaults.SEED_GOAL_TARGET.stripTrailingZeros().toPlainString(),
            goalCurrentText = emergencyGoal.currentAmount?.amount?.stripTrailingZeros()?.toPlainString() ?: "0",
            goalSkipped = false,
        )
    }

    fun hasExistingPlan(plan: FinancialPlan?): Boolean = plan != null
}
