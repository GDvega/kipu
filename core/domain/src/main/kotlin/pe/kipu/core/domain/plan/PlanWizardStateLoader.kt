package pe.kipu.core.domain.plan

import java.math.BigDecimal
import java.math.RoundingMode
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money

/**
 * Maps persisted plan data into wizard form defaults when re-editing.
 */
object PlanWizardStateLoader {

    data class IncomeDefaults(
        val approximateIncomeText: String,
        val fixedBaseText: String,
        val secondQuincenaText: String,
        val extraIncomeText: String,
        val lowWeekText: String,
        val normalWeekText: String,
        val goodWeekText: String,
        val initialBalanceText: String,
    )

    data class FixedExpenseDefaults(
        val skipFixedExpenses: Boolean,
        val electricityText: String,
        val waterText: String,
        val internetText: String,
        val rentText: String,
        val phoneText: String,
        val debtsText: String,
        val educationText: String,
        val customExpenseLines: List<PlanWizardLineItem> = emptyList(),
    )

    data class GoalDefaults(
        val goalType: GoalType,
        val goalName: String,
        val goalTargetText: String,
        val goalCurrentText: String,
        val goalMonthsText: String,
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
        val monthlyIncome = plan?.estimatedMonthlyIncome?.amount
        val defaults = IncomeDefaults(
            approximateIncomeText = monthlyIncome?.toInputText().orEmpty(),
            fixedBaseText = "",
            secondQuincenaText = "",
            extraIncomeText = "",
            lowWeekText = "",
            normalWeekText = "",
            goodWeekText = "",
            initialBalanceText = plan?.initialBalance?.amount?.toInputText().orEmpty(),
        )
        if (plan == null || monthlyIncome == null) return defaults

        return when (plan.incomeProfile) {
            IncomeProfile.APPROXIMATE -> defaults
            IncomeProfile.VARIABLE -> {
                val weeklyTotal = monthlyIncome
                    .multiply(VARIABLE_WEEKS)
                    .divide(WEEKLY_TO_MONTHLY, 2, RoundingMode.HALF_UP)
                val baseWeek = weeklyTotal.divide(VARIABLE_WEEKS, 2, RoundingMode.DOWN)
                defaults.copy(
                    lowWeekText = baseWeek.toInputText(),
                    normalWeekText = baseWeek.toInputText(),
                    goodWeekText = weeklyTotal
                        .subtract(baseWeek.multiply(VARIABLE_WEEKS))
                        .add(baseWeek)
                        .toInputText(),
                )
            }
            IncomeProfile.FIXED -> when (plan.payFrequency) {
                PayFrequency.MONTHLY -> defaults.copy(fixedBaseText = monthlyIncome.toInputText())
                PayFrequency.BIWEEKLY -> {
                    val firstQuincena = monthlyIncome.divide(BIWEEKLY_DIVISOR, 2, RoundingMode.UP)
                    defaults.copy(
                        fixedBaseText = firstQuincena.toInputText(),
                        secondQuincenaText = monthlyIncome.subtract(firstQuincena).toInputText(),
                    )
                }
                PayFrequency.WEEKLY -> {
                    val weeklyBase = monthlyIncome.divide(WEEKLY_TO_MONTHLY, 2, RoundingMode.DOWN)
                    val remainder = monthlyIncome.subtract(weeklyBase.multiply(WEEKLY_TO_MONTHLY))
                    defaults.copy(
                        fixedBaseText = weeklyBase.toInputText(),
                        extraIncomeText = remainder.takeUnless { it.signum() == 0 }?.toInputText().orEmpty(),
                    )
                }
            }
        }
    }

    fun fixedExpenseDefaults(plan: FinancialPlan?): FixedExpenseDefaults {
        val fixed = plan?.fixedExpenses
        if (fixed == null) {
            return FixedExpenseDefaults(
                skipFixedExpenses = false,
                electricityText = "",
                waterText = "",
                internetText = "",
                rentText = "",
                phoneText = "",
                debtsText = "",
                educationText = "",
            )
        }

        if (fixed.isZero()) {
            return FixedExpenseDefaults(
                skipFixedExpenses = true,
                electricityText = "",
                waterText = "",
                internetText = "",
                rentText = "",
                phoneText = "",
                debtsText = "",
                educationText = "",
            )
        }

        val hasBreakdown = plan.electricityExpenses != null ||
            plan.waterExpenses != null ||
            plan.internetExpenses != null ||
            plan.rentExpenses != null ||
            plan.phoneExpenses != null ||
            plan.debtsExpenses != null ||
            plan.educationExpenses != null ||
            !plan.customFixedExpensesJson.isNullOrBlank()

        if (hasBreakdown) {
            return FixedExpenseDefaults(
                skipFixedExpenses = false,
                electricityText = plan.electricityExpenses?.amount?.toInputText().orEmpty(),
                waterText = plan.waterExpenses?.amount?.toInputText().orEmpty(),
                internetText = plan.internetExpenses?.amount?.toInputText().orEmpty(),
                rentText = plan.rentExpenses?.amount?.toInputText().orEmpty(),
                phoneText = plan.phoneExpenses?.amount?.toInputText().orEmpty(),
                debtsText = plan.debtsExpenses?.amount?.toInputText().orEmpty(),
                educationText = plan.educationExpenses?.amount?.toInputText().orEmpty(),
                customExpenseLines = CustomFixedExpenseSerializer.deserialize(plan.customFixedExpensesJson),
            )
        }

        return FixedExpenseDefaults(
            skipFixedExpenses = false,
            electricityText = "",
            waterText = "",
            internetText = "",
            rentText = "",
            phoneText = "",
            debtsText = "",
            educationText = "",
        )
    }


    fun goalDefaults(emergencyGoal: Commitment?): GoalDefaults {
        if (emergencyGoal == null || emergencyGoal.type != CommitmentType.SAVINGS_GOAL || emergencyGoal.isSettled) {
            return GoalDefaults(
                goalType = GoalType.EMERGENCY,
                goalName = GoalType.EMERGENCY.defaultTitle(),
                goalTargetText = "",
                goalCurrentText = "",
                goalMonthsText = "5",
                goalSkipped = false,
            )
        }

        return GoalDefaults(
            goalType = if (emergencyGoal.currencyCode.equals(GoalCurrency.USD.code, ignoreCase = true)) {
                GoalType.DOLLARS
            } else {
                GoalType.EMERGENCY
            },
            goalName = emergencyGoal.title,
            goalTargetText = emergencyGoal.targetAmount?.amount?.stripTrailingZeros()?.toPlainString()
                ?: PeruPlanDefaults.SEED_GOAL_TARGET.stripTrailingZeros().toPlainString(),
            goalCurrentText = emergencyGoal.currentAmount?.amount?.stripTrailingZeros()?.toPlainString() ?: "0",
            goalMonthsText = emergencyGoal.savingsHorizonMonths?.toString().orEmpty(),
            goalSkipped = false,
        )
    }

    fun customEnvelopeDefaults(
        plan: FinancialPlan?,
        budgets: List<EnvelopeBudgetState>,
    ): List<PlanWizardLineItem> {
        val linkedEnvelopeIds = plan?.envelopeIds.orEmpty().toSet()
        return budgets
            .asSequence()
            .filter { budget ->
                budget.envelopeId in linkedEnvelopeIds &&
                    PlanEnvelopeTemplates.isWizardManagedCustomEnvelope(budget.envelopeId)
            }
            .sortedBy { it.envelopeId }
            .map { budget ->
                PlanWizardLineItem(
                    id = budget.envelopeId,
                    label = budget.name,
                    amountText = budget.weeklyLimit.amount.stripTrailingZeros().toPlainString(),
                    categoryId = budget.categoryId,
                )
            }
            .toList()
    }

    fun hasExistingPlan(plan: FinancialPlan?): Boolean = plan != null

    private fun BigDecimal.toInputText(): String = stripTrailingZeros().toPlainString()

    private val WEEKLY_TO_MONTHLY = BigDecimal("4")
    private val BIWEEKLY_DIVISOR = BigDecimal("2")
    private val VARIABLE_WEEKS = BigDecimal("3")
}
