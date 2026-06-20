package pe.kipu.core.domain.duplicate

/**
 * Tuning for duplicate movement detection (MVP).
 *
 * Standard match requires amount, counterparty and [recordedAt] within [TIME_TOLERANCE_MINUTES].
 * Strong match via equal non-blank [pe.kipu.core.domain.model.Movement.operationNumber] and same amount;
 * ignores time tolerance.
 */
object DuplicateDetectionConfig {
    const val TIME_TOLERANCE_MINUTES: Long = 15L
}
