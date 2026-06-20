package pe.kipu.core.data.mapper

import java.time.Instant
import pe.kipu.core.data.local.entity.GatheringExpenseEntity
import pe.kipu.core.domain.model.GatheringExpense

fun GatheringExpenseEntity.toDomain(): GatheringExpense = GatheringExpense(
    id = id,
    gatheringId = gatheringId,
    amount = amountCents.toMoney(),
    paidByParticipant = paidByParticipant,
    description = description,
    movementId = movementId,
    recordedAt = Instant.ofEpochMilli(recordedAtMillis),
)

fun GatheringExpense.toEntity(): GatheringExpenseEntity = GatheringExpenseEntity(
    id = id,
    gatheringId = gatheringId,
    amountCents = amount.toAmountCents(),
    paidByParticipant = paidByParticipant,
    description = description,
    movementId = movementId,
    recordedAtMillis = recordedAt.toEpochMilli(),
)
