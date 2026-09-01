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
        envelopeId: EntityId? = null,
        legacyEnvelopeIdForCategory: EntityId? = null,
    ): Money = movements
        .asSequence()
        .filter { movement ->
            belongsToEnvelopeOrCategory(
                movement = movement,
                categoryId = categoryId,
                envelopeId = envelopeId,
                legacyEnvelopeIdForCategory = legacyEnvelopeIdForCategory,
            ) &&
                movement.type == MovementType.EXPENSE &&
                movement.status == MovementStatus.CONFIRMED &&
                cycleRange.contains(movement.recordedAt) &&
                movement.id !in gatheringLinkedMovementIds
        }
        .fold(Money.ZERO) { total, movement -> total + movement.amount }

    private fun belongsToEnvelopeOrCategory(
        movement: Movement,
        categoryId: EntityId,
        envelopeId: EntityId?,
        legacyEnvelopeIdForCategory: EntityId?,
    ): Boolean {
        if (envelopeId == null) return movement.categoryId == categoryId
        return movement.envelopeId?.let { it == envelopeId }
            ?: (movement.categoryId == categoryId && legacyEnvelopeIdForCategory == envelopeId)
    }
}
