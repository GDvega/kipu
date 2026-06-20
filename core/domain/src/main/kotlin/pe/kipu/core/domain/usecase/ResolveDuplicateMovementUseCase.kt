package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.repository.MovementRepository

class ResolveDuplicateMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
) {

    suspend operator fun invoke(
        pair: MovementDuplicatePair,
        resolution: DuplicateResolution,
    ): Result<Unit> = when (resolution) {
        DuplicateResolution.MERGE -> {
            movementRepository.delete(selectMovementToDeleteOnMerge(pair).id)
        }

        DuplicateResolution.SAVE_AS_NEW,
        DuplicateResolution.CANCEL,
        -> Result.success(Unit)
    }

    private fun selectMovementToDeleteOnMerge(pair: MovementDuplicatePair): Movement {
        val createdAtCompare = pair.movementA.createdAt.compareTo(pair.movementB.createdAt)
        return when {
            createdAtCompare > 0 -> pair.movementA
            createdAtCompare < 0 -> pair.movementB
            pair.movementA.id >= pair.movementB.id -> pair.movementA
            else -> pair.movementB
        }
    }
}
