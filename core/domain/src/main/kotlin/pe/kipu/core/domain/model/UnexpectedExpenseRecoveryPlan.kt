package pe.kipu.core.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt

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
    val snapshot: UnexpectedExpenseSnapshot? = null,
) {
    fun recoveryPlanFor(selectedEnvelopeIds: Set<EntityId>): UnexpectedExpenseRecoveryPlan {
        val adjustments = recoveryPlan.adjustments.filter { it.envelopeId in selectedEnvelopeIds }
        val recovered = adjustments.fold(BigDecimal.ZERO) { total, adjustment ->
            total + adjustment.reduction.amount
        }
        val remaining = (coverage.uncovered.amount + coverage.existingShortfall.amount - recovered)
            .max(coverage.liquidityGap.amount)
        return UnexpectedExpenseRecoveryPlan(
            adjustments = adjustments,
            remainingGap = Money.of(remaining).getOrError(),
            isFullyRecoverable = remaining.signum() == 0,
        )
    }
}

/** Local confirmation token: compare actual financial inputs, never log or export it. */
data class UnexpectedExpenseSnapshot(
    val date: LocalDate,
    val expense: Money,
    val plan: FinancialPlan?,
    val movements: List<Movement>,
    val reserveEvents: List<ReserveEvent>,
    val commitments: List<Commitment>,
    val budgets: List<EnvelopeBudgetState>,
    val receipts: List<MonthlyServiceReceipt>,
)
