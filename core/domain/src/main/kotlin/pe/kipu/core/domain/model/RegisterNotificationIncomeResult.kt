package pe.kipu.core.domain.model

/**
 * Result of registering a parsed notification income as a pending movement.
 */
sealed interface RegisterNotificationIncomeResult {
    data class Saved(val movement: Movement) : RegisterNotificationIncomeResult

    data object AlreadyPending : RegisterNotificationIncomeResult

    data class ParseFailed(val error: DomainError) : RegisterNotificationIncomeResult

    data class ValidationFailed(val error: DomainError) : RegisterNotificationIncomeResult

    data class AutoApproved(val movement: Movement) : RegisterNotificationIncomeResult
}
