package pe.kipu.core.domain.model

sealed interface DuplicateDetectionResult {
    data object NoMatch : DuplicateDetectionResult

    data class Matches(val existing: List<Movement>) : DuplicateDetectionResult
}
