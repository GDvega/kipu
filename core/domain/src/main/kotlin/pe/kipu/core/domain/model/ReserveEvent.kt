package pe.kipu.core.domain.model

import java.time.Instant

enum class ReserveEventType {
    CONTRIBUTION,
    USE,
    REFUND,
    REVERSAL,
}

data class ReserveEvent(
    val id: EntityId,
    val type: ReserveEventType,
    val amount: Money,
    val sourceMovementId: EntityId? = null,
    val reversesEventId: EntityId? = null,
    val occurredAt: Instant,
    val createdAt: Instant,
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Reserve event id must not be blank"))
        amount.isZero() -> DomainResult.Err(DomainError.InvalidAmount("Reserve event amount must be positive"))
        sourceMovementId?.isBlank() == true ->
            DomainResult.Err(DomainError.InvalidId("Source movement id must not be blank"))
        reversesEventId?.isBlank() == true ->
            DomainResult.Err(DomainError.InvalidId("Reversed event id must not be blank"))
        type == ReserveEventType.USE && sourceMovementId == null ->
            DomainResult.Err(DomainError.InvalidField("Reserve use requires a source movement"))
        type == ReserveEventType.REFUND && sourceMovementId == null ->
            DomainResult.Err(DomainError.InvalidField("Reserve refund requires a source movement"))
        type == ReserveEventType.REVERSAL && reversesEventId == null ->
            DomainResult.Err(DomainError.InvalidField("Reserve reversal requires a referenced event"))
        type != ReserveEventType.REVERSAL && reversesEventId != null ->
            DomainResult.Err(DomainError.InvalidField("Only a reversal can reference another reserve event"))
        else -> DomainResult.Ok(Unit)
    }
}
