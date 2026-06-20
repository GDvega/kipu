package pe.kipu.core.domain.model

/**
 * Thrown when confirming a suggested movement would create a duplicate
 * without an explicit [DuplicateResolution].
 */
class DuplicateConfirmationRequiredException(
    message: String = "Duplicate movement requires explicit resolution before saving",
) : IllegalStateException(message)
