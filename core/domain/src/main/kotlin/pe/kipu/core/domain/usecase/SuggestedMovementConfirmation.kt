package pe.kipu.core.domain.usecase

import java.time.Instant
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.SuggestedMovement

internal fun buildConfirmedMovementFromSuggestion(
    suggestion: SuggestedMovement,
    movementId: EntityId,
    categoryId: EntityId,
    createdAt: Instant,
    recordedAt: Instant,
): DomainResult<Movement> {
    val movement = Movement(
        id = movementId,
        type = suggestion.type ?: MovementType.EXPENSE,
        amount = suggestion.amount
            ?: return DomainResult.Err(
                pe.kipu.core.domain.model.DomainError.InvalidField("Amount is required to confirm"),
            ),
        categoryId = categoryId,
        channel = suggestion.channel ?: PaymentChannel.OTHER,
        source = suggestion.source,
        status = MovementStatus.CONFIRMED,
        description = suggestion.message,
        counterpartyName = suggestion.counterpartyName,
        operationNumber = suggestion.operationReference,
        recordedAt = recordedAt,
        createdAt = createdAt,
    )
    return when (val validation = movement.validate()) {
        is DomainResult.Ok -> DomainResult.Ok(movement)
        is DomainResult.Err -> DomainResult.Err(validation.error)
    }
}
