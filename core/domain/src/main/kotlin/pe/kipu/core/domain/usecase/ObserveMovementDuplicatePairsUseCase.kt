package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.duplicate.canonicalKey
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.repository.DuplicateDismissalRepository
import pe.kipu.core.domain.repository.MovementRepository

class ObserveMovementDuplicatePairsUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val duplicateDismissalRepository: DuplicateDismissalRepository,
    private val findMovementDuplicatePairs: FindMovementDuplicatePairsUseCase,
) {

    operator fun invoke(): Flow<List<MovementDuplicatePair>> =
        combine(
            movementRepository.observeMovements(),
            duplicateDismissalRepository.observeDismissedPairKeys(),
        ) { movements, dismissedPairKeys ->
            findMovementDuplicatePairs(movements).filter { pair ->
                pair.canonicalKey() !in dismissedPairKeys
            }
        }
}
