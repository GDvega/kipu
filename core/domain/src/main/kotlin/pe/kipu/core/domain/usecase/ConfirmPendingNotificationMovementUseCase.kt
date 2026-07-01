package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DuplicateDetectionResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.MovementRepository

/**
 * Promotes a pending notification movement to [MovementStatus.CONFIRMED]
 * after duplicate detection (same semantics as F10).
 */
class ConfirmPendingNotificationMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val detectDuplicateMovement: DetectDuplicateMovementUseCase,
) {

    suspend operator fun invoke(
        movementId: EntityId,
        resolution: DuplicateResolution? = null,
    ): ConfirmMovementResult {
        val pending = movementRepository.getById(movementId)
            ?: return ConfirmMovementResult.Cancelled

        if (pending.status != MovementStatus.PENDING_CONFIRMATION ||
            pending.source != MovementSource.NOTIFICATION
        ) {
            return ConfirmMovementResult.Cancelled
        }

        val candidate = pending.copy(status = MovementStatus.CONFIRMED)
        val existingMovements = movementRepository.observeMovements().first()
        val detection = detectDuplicateMovement(candidate, existingMovements)

        return when (detection) {
            DuplicateDetectionResult.NoMatch -> {
                movementRepository.save(candidate).getOrThrow()
                ConfirmMovementResult.Saved(candidate)
            }

            is DuplicateDetectionResult.Matches -> when (resolution) {
                null -> ConfirmMovementResult.DuplicatePending(
                    candidate = candidate,
                    matches = detection.existing,
                )

                DuplicateResolution.SAVE_AS_NEW -> {
                    movementRepository.save(candidate).getOrThrow()
                    ConfirmMovementResult.Saved(candidate)
                }

                DuplicateResolution.MERGE -> {
                    movementRepository.delete(movementId).getOrThrow()
                    ConfirmMovementResult.Cancelled
                }

                DuplicateResolution.CANCEL -> ConfirmMovementResult.Cancelled
            }
        }
    }

    private fun Result<Unit>.getOrThrow() {
        getOrElse { throw IllegalStateException("Failed to save movement") }
    }
}
