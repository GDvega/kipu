package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.MovementStatus

class FindMovementDuplicatePairsUseCase @Inject constructor(
    private val matcher: MovementDuplicateMatcher,
) {

    operator fun invoke(movements: List<Movement>): List<MovementDuplicatePair> {
        val confirmed = movements
            .filter { it.status == MovementStatus.CONFIRMED }
            .sortedBy { it.id }
        val pairs = mutableListOf<MovementDuplicatePair>()

        for (index in confirmed.indices) {
            for (otherIndex in index + 1 until confirmed.size) {
                val movementA = confirmed[index]
                val movementB = confirmed[otherIndex]

                val reasonKey = matcher.findMatchReasonKey(movementA, movementB) ?: continue
                pairs += MovementDuplicatePair(
                    movementA = movementA,
                    movementB = movementB,
                    matchReasonKey = reasonKey,
                )
            }
        }

        return pairs
    }
}
