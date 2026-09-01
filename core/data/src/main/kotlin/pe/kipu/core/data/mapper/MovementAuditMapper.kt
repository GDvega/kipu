package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.converter.KipuTypeConverters
import pe.kipu.core.data.local.entity.MovementAuditEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import java.math.BigDecimal
import java.time.Instant

private val enumConverters = KipuTypeConverters()

fun MovementAuditEntity.toDomain(): MovementAuditEntry = MovementAuditEntry(
    id = id,
    movementId = movementId,
    action = when (action) {
        MovementAuditAction.CREATED.name -> MovementAuditAction.CREATED
        MovementAuditAction.UPDATED.name -> MovementAuditAction.UPDATED
        MovementAuditAction.DELETED.name -> MovementAuditAction.DELETED
        else -> MovementAuditAction.CREATED
    },
    movementType = when (movementType) {
        MovementType.INCOME.name -> MovementType.INCOME
        else -> MovementType.EXPENSE
    },
    amount = amountCents.toMoney(),
    categoryId = categoryId,
    categoryName = categoryName,
    channel = enumConverters.stringToPaymentChannel(channel) ?: PaymentChannel.OTHER,
    description = description,
    counterpartyName = counterpartyName,
    details = details,
    timestamp = Instant.ofEpochMilli(timestampEpochMs),
)

fun MovementAuditEntry.toEntity(): MovementAuditEntity = MovementAuditEntity(
    id = id,
    movementId = movementId,
    action = action.name,
    movementType = movementType.name,
    amountCents = amount.toAmountCents(),
    categoryId = categoryId,
    categoryName = categoryName,
    channel = channel.name,
    description = description,
    counterpartyName = counterpartyName,
    details = details,
    timestampEpochMs = timestamp.toEpochMilli(),
)
