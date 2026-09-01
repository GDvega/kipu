package pe.kipu.core.domain.model

import java.math.BigDecimal

data class RecoveryEnvelopeAdjustment(
    val envelopeId: EntityId,
    val envelopeName: String,
    val currentLimit: Money,
    val spentAmount: Money,
    val proposedLimit: Money,
    val reduction: Money,
)

data class UnexpectedExpenseRecoveryPlan(
    val adjustments: List<RecoveryEnvelopeAdjustment>,
    val remainingGap: Money,
    val isFullyRecoverable: Boolean,
)

data class UnexpectedExpensePreview(
    val coverage: UnexpectedExpenseCoverage,
    val recoveryPlan: UnexpectedExpenseRecoveryPlan,
) {
    fun recoveryPlanFor(selectedEnvelopeIds: Set<EntityId>): UnexpectedExpenseRecoveryPlan {
        val adjustments = recoveryPlan.adjustments.filter { it.envelopeId in selectedEnvelopeIds }
        val recovered = adjustments.fold(BigDecimal.ZERO) { total, adjustment ->
            total + adjustment.reduction.amount
        }
        val remaining = (coverage.uncovered.amount - recovered).max(BigDecimal.ZERO)
        return UnexpectedExpenseRecoveryPlan(
            adjustments = adjustments,
            remainingGap = Money.of(remaining).getOrError(),
            isFullyRecoverable = remaining.signum() == 0,
        )
    }
}
