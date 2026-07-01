package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.time.CycleRange

class CalculateCategoryPeriodSpentUseCase @Inject constructor() {

    operator fun invoke(
        categoryId: EntityId,
        movements: List<Movement>,
        cycleRange: CycleRange,
        gatheringLinkedMovementIds: Set<EntityId> = emptySet(),
    ): Money = movements
        .asSequence()
        .filter { movement ->
            movement.categoryId == categoryId &&
                movement.type == MovementType.EXPENSE &&
                movement.status == MovementStatus.CONFIRMED &&
                cycleRange.contains(movement.recordedAt) &&
                movement.id !in gatheringLinkedMovementIds
        }
        .fold(Money.ZERO) { total, movement -> total + movement.amount }
}
