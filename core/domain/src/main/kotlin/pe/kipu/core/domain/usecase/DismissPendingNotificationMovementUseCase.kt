package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.MovementRepository

class DismissPendingNotificationMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
) {

    suspend operator fun invoke(movementId: EntityId): Boolean {
        val pending = movementRepository.getById(movementId) ?: return false
        if (pending.status != MovementStatus.PENDING_CONFIRMATION ||
            pending.source != MovementSource.NOTIFICATION
        ) {
            return false
        }
        return movementRepository.delete(movementId).isSuccess
    }
}
