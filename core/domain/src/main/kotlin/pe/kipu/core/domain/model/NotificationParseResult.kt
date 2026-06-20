package pe.kipu.core.domain.model

/**
 * Result of parsing text from a payment app notification.
 */
sealed interface NotificationParseResult {
    data class Success(val suggestion: SuggestedMovement) : NotificationParseResult

    data class Failure(val error: DomainError) : NotificationParseResult

    data object UnsupportedChannel : NotificationParseResult
}
