package pe.kipu.core.domain.model

sealed interface ConfirmMovementResult {
    data class Saved(val movement: Movement) : ConfirmMovementResult

    data class DuplicatePending(
        val candidate: Movement,
        val matches: List<Movement>,
    ) : ConfirmMovementResult

    data object Cancelled : ConfirmMovementResult
}
