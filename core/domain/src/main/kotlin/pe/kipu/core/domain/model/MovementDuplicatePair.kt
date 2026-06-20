package pe.kipu.core.domain.model

/**
 * Two confirmed movements suspected to represent the same transaction.
 */
data class MovementDuplicatePair(
    val movementA: Movement,
    val movementB: Movement,
    val matchReasonKey: String,
)
