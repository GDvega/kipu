package pe.kipu.core.domain.duplicate

import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementDuplicatePair

fun canonicalMovementDuplicatePairKey(
    movementAId: EntityId,
    movementBId: EntityId,
): String {
    val ids = listOf(movementAId, movementBId).sorted()
    return "${ids[0]}:${ids[1]}"
}

fun MovementDuplicatePair.canonicalKey(): String =
    canonicalMovementDuplicatePairKey(movementA.id, movementB.id)
