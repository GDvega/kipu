package pe.kipu.core.domain.usecase

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DuplicateDetectionResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner

/**
 * Promotes a pending notification movement to [MovementStatus.CONFIRMED]
 * after duplicate detection (same semantics as F10) and records an audit log.
 */
class ConfirmPendingNotificationMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val detectDuplicateMovement: DetectDuplicateMovementUseCase,
    private val movementAuditRepository: MovementAuditRepository,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
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
                saveWithAudit(candidate)
                ConfirmMovementResult.Saved(candidate)
            }

            is DuplicateDetectionResult.Matches -> when (resolution) {
                null -> ConfirmMovementResult.DuplicatePending(
                    candidate = candidate,
                    matches = detection.existing,
                )

                DuplicateResolution.SAVE_AS_NEW -> {
                    saveWithAudit(candidate)
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

    private suspend fun saveWithAudit(movement: pe.kipu.core.domain.model.Movement) {
        val auditEntry = MovementAuditEntry(
            id = UUID.randomUUID().toString(),
            movementId = movement.id,
            action = MovementAuditAction.CREATED,
            movementType = movement.type,
            amount = movement.amount,
            categoryId = movement.categoryId,
            channel = movement.channel,
            description = movement.description,
            counterpartyName = movement.counterpartyName,
            details = "Ingreso por notificación confirmado",
            timestamp = movement.createdAt,
        )
        localTransactionRunner.run {
            movementRepository.save(movement).getOrThrow()
            movementAuditRepository.recordAudit(auditEntry).getOrThrow()
        }.getOrThrow()
    }

    private fun Result<Unit>.getOrThrow() {
        getOrElse { throw IllegalStateException("Failed to save movement") }
    }
}
