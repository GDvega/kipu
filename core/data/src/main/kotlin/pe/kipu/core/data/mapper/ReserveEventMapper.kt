package pe.kipu.core.data.mapper

import java.time.Instant
import pe.kipu.core.data.local.entity.ReserveEventEntity
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType

fun ReserveEventEntity.toDomain(): ReserveEvent = ReserveEvent(
    id = id,
    type = ReserveEventType.valueOf(type),
    amount = amountCents.toMoney(),
    sourceMovementId = sourceMovementId,
    reversesEventId = reversesEventId,
    occurredAt = Instant.ofEpochMilli(occurredAtMillis),
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun ReserveEvent.toEntity(): ReserveEventEntity = ReserveEventEntity(
    id = id,
    type = type.name,
    amountCents = amount.toAmountCents(),
    sourceMovementId = sourceMovementId,
    reversesEventId = reversesEventId,
    occurredAtMillis = occurredAt.toEpochMilli(),
    createdAtMillis = createdAt.toEpochMilli(),
)
