package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentStatusKeys
import pe.kipu.core.domain.model.CommitmentSummary
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.SavingsGoalProgress
import pe.kipu.core.domain.repository.CommitmentRepository

class ObserveCommitmentSummariesUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val calculateSavingsGoalProgress: CalculateSavingsGoalProgressUseCase,
) {

    operator fun invoke(): Flow<List<CommitmentSummary>> =
        commitmentRepository.observeCommitments().map { commitments ->
            commitments.map(::toSummary)
        }

    private fun toSummary(commitment: Commitment): CommitmentSummary {
        val savingsProgress = when (commitment.type) {
            CommitmentType.SAVINGS_GOAL ->
                when (val result = calculateSavingsGoalProgress(commitment)) {
                    is DomainResult.Ok -> result.value
                    is DomainResult.Err -> null
                }

            else -> null
        }

        return CommitmentSummary(
            commitment = commitment,
            savingsProgress = savingsProgress,
            statusKey = resolveStatusKey(commitment, savingsProgress),
        )
    }

    private fun resolveStatusKey(
        commitment: Commitment,
        savingsProgress: SavingsGoalProgress?,
    ): String = when (commitment.type) {
        CommitmentType.SAVINGS_GOAL ->
            if (savingsProgress?.isCompleted == true || commitment.isSettled) {
                CommitmentStatusKeys.SAVINGS_COMPLETED
            } else {
                CommitmentStatusKeys.SAVINGS_IN_PROGRESS
            }

        CommitmentType.SOCIAL_DEBT ->
            if (commitment.isSettled) {
                CommitmentStatusKeys.SOCIAL_DEBT_SETTLED
            } else {
                CommitmentStatusKeys.SOCIAL_DEBT_PENDING
            }

        CommitmentType.PENDING_PAYMENT ->
            if (commitment.isSettled) {
                CommitmentStatusKeys.PENDING_PAYMENT_SETTLED
            } else {
                CommitmentStatusKeys.PENDING_PAYMENT_DUE
            }
    }
}
