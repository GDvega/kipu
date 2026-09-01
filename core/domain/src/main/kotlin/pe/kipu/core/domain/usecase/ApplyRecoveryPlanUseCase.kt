package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.LocalTransactionRunner

class ApplyRecoveryPlanUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    suspend operator fun invoke(proposal: UnexpectedExpenseRecoveryPlan): Result<Unit> {
        if (proposal.adjustments.map { it.envelopeId }.distinct().size != proposal.adjustments.size) {
            return Result.failure(IllegalArgumentException("Recovery proposal contains duplicate envelopes"))
        }
        return localTransactionRunner.run {
            proposal.adjustments.forEach { adjustment ->
                require(adjustment.proposedLimit.amount >= adjustment.spentAmount.amount) {
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
                envelopeRepository.save(current.copy(weeklyLimit = adjustment.proposedLimit)).getOrThrow()
            }
        }.map { Unit }
    }
}
