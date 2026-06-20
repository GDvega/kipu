package pe.kipu.core.domain.model

enum class MovementStatus {
    /** Saved movement confirmed by the user. */
    CONFIRMED,

    /** Inferred movement awaiting human review (OCR, notification, import). */
    PENDING_CONFIRMATION,
}
