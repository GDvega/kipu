package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.MovementRepository

class ObservePendingNotificationMovementsUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
) {

    operator fun invoke(): Flow<List<Movement>> =
        movementRepository.observeMovements().map { movements ->
            movements.filter { movement ->
                movement.status == MovementStatus.PENDING_CONFIRMATION &&
                    movement.source == MovementSource.NOTIFICATION
            }
        }
}
