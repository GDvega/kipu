package pe.kipu.core.domain.usecase

import java.util.UUID
import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner
import pe.kipu.core.domain.time.TimeProvider

/**
 * Deletes a movement by ID and records a DELETED audit log.
 * Cascading removals in linked tables (e.g. gathering expenses) are handled at the database level.
 */
class DeleteMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val movementAuditRepository: MovementAuditRepository,
    private val reserveEventRepository: ReserveEventRepository,
    private val timeProvider: TimeProvider,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    suspend operator fun invoke(movementId: EntityId): Result<Unit> {
        if (movementId.isBlank()) {
            return Result.failure(IllegalArgumentException("Movement id must not be blank"))
        }

        val existing = movementRepository.getById(movementId)
        if (existing == null) return movementRepository.delete(movementId)
        val activeReserveUse = reserveEventRepository.activeUseForMovement(movementId)
        val now = timeProvider.now()

        val auditEntry = MovementAuditEntry(
            id = UUID.randomUUID().toString(),
            movementId = existing.id,
            action = MovementAuditAction.DELETED,
            movementType = existing.type,
            amount = existing.amount,
            categoryId = existing.categoryId,
            channel = existing.channel,
            description = existing.description,
            counterpartyName = existing.counterpartyName,
            details = "Registro eliminado",
            timestamp = now,
        )
        return localTransactionRunner.run {
            movementRepository.delete(movementId).getOrThrow()
            movementAuditRepository.recordAudit(auditEntry).getOrThrow()
            activeReserveUse?.let { reserveEventRepository.record(it.reversal(now)).getOrThrow() }
        }.map { Unit }
    }
}
