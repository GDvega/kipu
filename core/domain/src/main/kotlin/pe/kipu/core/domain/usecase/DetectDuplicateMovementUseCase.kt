package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.DuplicateDetectionResult
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus

class DetectDuplicateMovementUseCase @Inject constructor(
    private val matcher: MovementDuplicateMatcher,
) {

    operator fun invoke(
        candidate: Movement,
        existingMovements: List<Movement>,
    ): DuplicateDetectionResult {
        if (candidate.status != MovementStatus.CONFIRMED) {
            return DuplicateDetectionResult.NoMatch
        }

        val matches = existingMovements
            .filter { it.id != candidate.id && it.status == MovementStatus.CONFIRMED }
            .filter { matcher.isDuplicate(candidate, it) }

        return if (matches.isEmpty()) {
            DuplicateDetectionResult.NoMatch
        } else {
            DuplicateDetectionResult.Matches(matches)
        }
    }
}
