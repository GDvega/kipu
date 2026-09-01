package pe.kipu.core.domain.usecase

import java.util.UUID
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.repository.ReserveEventRepository
import java.time.Instant

internal suspend fun ReserveEventRepository.activeUseForMovement(
    movementId: EntityId,
): ReserveEvent? {
    val events = observeAll().first()
    val reversedIds = events
        .asSequence()
        .filter { it.type == ReserveEventType.REVERSAL }
        .mapNotNull { it.reversesEventId }
        .toSet()
    return events.firstOrNull { event ->
        event.type == ReserveEventType.USE &&
            event.sourceMovementId == movementId &&
            event.id !in reversedIds
    }
}

internal fun ReserveEvent.reversal(now: Instant): ReserveEvent = ReserveEvent(
    id = "reserve-reversal-${UUID.randomUUID()}",
    type = ReserveEventType.REVERSAL,
    amount = amount,
    reversesEventId = id,
    occurredAt = now,
    createdAt = now,
)

internal fun replacementReserveUse(
    movementId: EntityId,
    amount: Money,
    now: Instant,
): ReserveEvent = ReserveEvent(
    id = "reserve-use-${UUID.randomUUID()}",
    type = ReserveEventType.USE,
    amount = amount,
    sourceMovementId = movementId,
    occurredAt = now,
    createdAt = now,
)
