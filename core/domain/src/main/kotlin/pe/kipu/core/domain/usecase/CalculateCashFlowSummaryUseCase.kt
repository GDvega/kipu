package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.CashFlowSummary
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType

class CalculateCashFlowSummaryUseCase @Inject constructor() {

    operator fun invoke(
        movements: List<Movement>,
        commitments: List<Commitment>,
    ): CashFlowSummary {
        val confirmedMovements = movements.filter { it.status == MovementStatus.CONFIRMED }
        
        val totalIncome = confirmedMovements
            .filter { it.type == MovementType.INCOME }
            .fold(Money.ZERO) { total, movement -> total + movement.amount }
            
        val totalExpenses = confirmedMovements
            .filter { it.type == MovementType.EXPENSE }
            .fold(Money.ZERO) { total, movement -> total + movement.amount }
            
        val netCash = when (val result = totalIncome - totalExpenses) {
            is pe.kipu.core.domain.model.DomainResult.Ok -> result.value
            is pe.kipu.core.domain.model.DomainResult.Err -> Money.ZERO
        }
        
        val totalGoalTarget = commitments
            .filter { it.type == CommitmentType.SAVINGS_GOAL && !it.isSettled }
            .mapNotNull { it.targetAmount }
            .fold(Money.ZERO) { total, amount -> total + amount }
            
        val isGoalAtRisk = netCash.amount < totalGoalTarget.amount

        return CashFlowSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netCash = netCash,
            totalGoalTarget = totalGoalTarget,
            isGoalAtRisk = isGoalAtRisk,
        )
    }
}
