package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentStatusKeys
import pe.kipu.core.domain.model.CommitmentSummary
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.SavingsGoalProgress
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.MovementRepository

class ObserveCommitmentSummariesUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val movementRepository: MovementRepository,
    private val calculateSavingsGoalProgress: CalculateSavingsGoalProgressUseCase,
    private val calculateCashFlowSummary: CalculateCashFlowSummaryUseCase,
) {

    operator fun invoke(): Flow<List<CommitmentSummary>> =
        commitmentRepository.observeCommitments()
            .combine(movementRepository.observeMovements()) { commitments, movements ->
                val cashFlowSummary = calculateCashFlowSummary(movements, commitments)
                commitments.map { commitment -> toSummary(commitment, movements, cashFlowSummary) }
            }

    private fun toSummary(
        commitment: Commitment,
        movements: List<Movement>,
        cashFlowSummary: pe.kipu.core.domain.model.CashFlowSummary,
    ): CommitmentSummary {
        val savingsProgress = when (commitment.type) {
            CommitmentType.SAVINGS_GOAL -> {
                val linkedIncome = CommitmentLinkedIncomeCalculator.sumLinkedIncome(commitment.id, movements)
                when (val result = calculateSavingsGoalProgress(commitment, linkedIncome)) {
                    is DomainResult.Ok -> result.value
                    is DomainResult.Err -> null
                }
            }

            else -> null
        }
        
        val isAtRisk = commitment.type == CommitmentType.SAVINGS_GOAL && 
            !commitment.isSettled && 
            cashFlowSummary.isGoalAtRisk

        return CommitmentSummary(
            commitment = commitment,
            savingsProgress = savingsProgress,
            statusKey = resolveStatusKey(commitment, savingsProgress),
            isAtRisk = isAtRisk,
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
