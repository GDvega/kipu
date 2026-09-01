package pe.kipu.core.domain.usecase

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.DuplicateDetectionResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner

/**
 * Confirms a suggested movement after duplicate detection and records an audit log.
 * Never persists when a duplicate is pending without an explicit [resolution].
 */
class ConfirmSuggestedMovementWithDuplicateCheckUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val detectDuplicateMovement: DetectDuplicateMovementUseCase,
    private val movementAuditRepository: MovementAuditRepository,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {

    suspend operator fun invoke(
        suggestion: SuggestedMovement,
        movementId: EntityId,
        categoryId: EntityId,
        createdAt: Instant,
        recordedAt: Instant? = null,
        resolution: DuplicateResolution? = null,
    ): ConfirmMovementResult {
        when (val suggestionValidation = suggestion.validate()) {
            is DomainResult.Err ->
                throw IllegalArgumentException(suggestionValidation.error.message)
            is DomainResult.Ok -> Unit
        }

        val resolvedRecordedAt = recordedAt ?: suggestion.suggestedRecordedAt
            ?: throw IllegalArgumentException("Recorded at is required to confirm")

        val candidate = when (
            val buildResult = buildConfirmedMovementFromSuggestion(
                suggestion = suggestion,
                movementId = movementId,
                categoryId = categoryId,
                createdAt = createdAt,
                recordedAt = resolvedRecordedAt,
            )
        ) {
            is DomainResult.Err ->
                throw IllegalArgumentException(buildResult.error.message)
            is DomainResult.Ok -> buildResult.value
        }

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

                DuplicateResolution.MERGE,
                DuplicateResolution.CANCEL,
                -> ConfirmMovementResult.Cancelled
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
            details = if (movement.type == pe.kipu.core.domain.model.MovementType.EXPENSE) {
                "Comprobante confirmado"
            } else {
                "Ingreso confirmado"
            },
            timestamp = movement.createdAt,
        )
        localTransactionRunner.run {
            movementRepository.save(movement).getOrThrow()
            movementAuditRepository.recordAudit(auditEntry).getOrThrow()
        }.getOrThrow()
    }
}
