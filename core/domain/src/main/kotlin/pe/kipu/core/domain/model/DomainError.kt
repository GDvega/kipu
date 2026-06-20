package pe.kipu.core.domain.model

/**
 * Domain-level errors for predictable validation and business rules.
 */
sealed interface DomainError {
    val message: String

    data class InvalidAmount(override val message: String) : DomainError

    data class InvalidId(override val message: String) : DomainError

    data class InvalidField(override val message: String) : DomainError

    data class NotFound(override val message: String) : DomainError

    data class PersistenceFailed(override val message: String) : DomainError
}

/**
 * Result type for domain operations that must not throw for expected failures.
 */
sealed interface DomainResult<out T> {
    data class Ok<T>(val value: T) : DomainResult<T>

    data class Err(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T> domainResultOf(block: () -> T): DomainResult<T> =
    try {
        DomainResult.Ok(block())
    } catch (@Suppress("TooGenericExceptionCaught") unexpected: Exception) {
        DomainResult.Err(DomainError.InvalidField(unexpected.message ?: "Unexpected error"))
    }

fun <T> DomainResult<T>.getOrError(): T = when (this) {
    is DomainResult.Ok -> value
    is DomainResult.Err -> error("Domain error: ${error.message}")
}
