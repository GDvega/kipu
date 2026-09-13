package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.LocalTransactionRunner

class ApplyRecoveryPlanUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    /** Caller reads current budgets inside the same transaction as registration. */
    suspend operator fun invoke(
        proposal: UnexpectedExpenseRecoveryPlan,
        currentBudgets: List<EnvelopeBudgetState>,
    ): Result<Unit> {
        if (proposal.adjustments.map { it.envelopeId }.distinct().size != proposal.adjustments.size) {
            return Result.failure(IllegalArgumentException("Recovery proposal contains duplicate envelopes"))
        }
        return localTransactionRunner.run {
            val updates = proposal.adjustments.map { adjustment ->
                val budget = requireNotNull(currentBudgets.singleOrNull { it.envelopeId == adjustment.envelopeId })
                require(adjustment.proposedLimit.amount >= budget.spentAmount.amount &&
                    !adjustment.proposedLimit.isZero() && !adjustment.reduction.isZero()) {
                    "Recovery proposal cannot go below recorded spending"
                }
                require(
                    adjustment.currentLimit.amount - adjustment.proposedLimit.amount ==
                        adjustment.reduction.amount,
                ) { "Recovery proposal amounts are inconsistent" }
                val current = requireNotNull(envelopeRepository.getById(adjustment.envelopeId)) {
                    "Recovery envelope no longer exists"
                }
                require(current.cycleLimit.amount == adjustment.currentLimit.amount) {
                    "Recovery proposal is stale"
                }
                current.copy(weeklyLimit = adjustment.proposedLimit)
            }
            updates.forEach { envelopeRepository.save(it).getOrThrow() }
        }.map { Unit }
    }
}
