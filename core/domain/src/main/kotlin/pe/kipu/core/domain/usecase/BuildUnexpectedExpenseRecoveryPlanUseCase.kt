package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.RecoveryEnvelopeAdjustment
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds

class BuildUnexpectedExpenseRecoveryPlanUseCase @Inject constructor() {
    operator fun invoke(
        uncovered: Money,
        budgets: List<EnvelopeBudgetState>,
    ): UnexpectedExpenseRecoveryPlan {
        var remaining = uncovered.amount
        val byId = budgets.associateBy { it.envelopeId }
        val adjustments = buildList {
            RECOVERY_ORDER.forEach { envelopeId ->
                val budget = byId[envelopeId] ?: return@forEach
                val minimumLimit = budget.spentAmount.amount.max(MINIMUM_ENVELOPE_LIMIT)
                val reducible = (budget.cycleLimit.amount - minimumLimit).max(BigDecimal.ZERO)
                val reduction = remaining.min(reducible)
                if (reduction.signum() <= 0) return@forEach
                add(
                    RecoveryEnvelopeAdjustment(
                        envelopeId = budget.envelopeId,
                        envelopeName = budget.name,
                        currentLimit = budget.cycleLimit,
                        spentAmount = budget.spentAmount,
                        proposedLimit = money(budget.cycleLimit.amount - reduction),
                        reduction = money(reduction),
                    ),
                )
                remaining -= reduction
            }
        }

        return UnexpectedExpenseRecoveryPlan(
            adjustments = adjustments,
            remainingGap = money(remaining),
            isFullyRecoverable = remaining.signum() == 0,
        )
    }

    private fun money(value: BigDecimal): Money = Money.of(value).getOrError()

    private companion object {
        val MINIMUM_ENVELOPE_LIMIT = BigDecimal("0.01")
        val RECOVERY_ORDER = listOf(
            DefaultPlanEnvelopeIds.ANT_SPENDING,
            DefaultPlanEnvelopeIds.LEISURE,
            DefaultPlanEnvelopeIds.FAMILY,
        )
    }
}
