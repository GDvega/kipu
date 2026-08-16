package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.CashFlowSummary
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType

class CalculateCashFlowSummaryUseCase @Inject constructor() {

    operator fun invoke(
        movements: List<Movement>,
        commitments: List<Commitment>,
        initialBalance: Money = Money.ZERO,
    ): CashFlowSummary {
        val confirmedMovements = movements.filter { it.status == MovementStatus.CONFIRMED }
        
        val totalIncome = confirmedMovements
            .filter { it.type == MovementType.INCOME }
            .fold(Money.ZERO) { total, movement -> total + movement.amount }
            
        val totalExpenses = confirmedMovements
            .filter { it.type == MovementType.EXPENSE }
            .fold(Money.ZERO) { total, movement -> total + movement.amount }
            
        val netCash = initialBalance.amount.add(totalIncome.amount).subtract(totalExpenses.amount)
        
        val totalGoalRemaining = commitments
            .filter { it.type == CommitmentType.SAVINGS_GOAL && !it.isSettled }
            .mapNotNull { commitment ->
                val target = commitment.targetAmount ?: return@mapNotNull null
                when (val remaining = target - (commitment.currentAmount ?: Money.ZERO)) {
                    is DomainResult.Ok -> remaining.value
                    is DomainResult.Err -> Money.ZERO
                }
            }
            .fold(Money.ZERO) { total, amount -> total + amount }
            
        val isGoalAtRisk = !totalGoalRemaining.isZero() && netCash < totalGoalRemaining.amount

        return CashFlowSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netCash = netCash,
            totalGoalRemaining = totalGoalRemaining,
            isGoalAtRisk = isGoalAtRisk,
        )
    }
}
