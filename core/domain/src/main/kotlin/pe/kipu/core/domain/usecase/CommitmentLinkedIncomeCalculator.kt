package pe.kipu.core.domain.usecase

import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType

object CommitmentLinkedIncomeCalculator {

    fun sumLinkedIncome(commitmentId: EntityId, movements: List<Movement>): Money =
        movements.asSequence()
            .filter {
                it.commitmentId == commitmentId &&
                    it.type == MovementType.INCOME &&
                    it.status == MovementStatus.CONFIRMED
            }
            .fold(Money.ZERO) { acc, movement -> acc + movement.amount }
}
