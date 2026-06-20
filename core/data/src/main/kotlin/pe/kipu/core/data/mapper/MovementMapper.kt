package pe.kipu.core.data.mapper

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import pe.kipu.core.data.local.converter.KipuTypeConverters
import pe.kipu.core.data.local.entity.MovementEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel

private val enumConverters = KipuTypeConverters()

fun MovementEntity.toDomain(): Movement {
    val money = amountCents.toMoney()
    return Movement(
        id = id,
        type = enumConverters.stringToMovementType(type) ?: MovementType.EXPENSE,
        amount = money,
        categoryId = categoryId,
        channel = enumConverters.stringToPaymentChannel(channel) ?: PaymentChannel.OTHER,
        source = enumConverters.stringToMovementSource(source) ?: MovementSource.MANUAL,
        status = enumConverters.stringToMovementStatus(status) ?: MovementStatus.CONFIRMED,
        description = description,
        counterpartyName = counterpartyName,
        operationNumber = operationNumber,
        recordedAt = Instant.ofEpochMilli(recordedAtMillis),
        createdAt = Instant.ofEpochMilli(createdAtMillis),
    )
}

fun Movement.toEntity(): MovementEntity = MovementEntity(
    id = id,
    type = type.name,
    amountCents = amount.toAmountCents(),
    categoryId = categoryId,
    channel = channel.name,
    source = source.name,
    status = status.name,
    description = description,
    counterpartyName = counterpartyName,
    operationNumber = operationNumber,
    recordedAtMillis = recordedAt.toEpochMilli(),
    createdAtMillis = createdAt.toEpochMilli(),
)

internal fun Money.toAmountCents(): Long =
    amount.multiply(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

internal fun Long.toMoney(): Money {
    val value = BigDecimal.valueOf(this).movePointLeft(2)
    return when (val result = Money.of(value)) {
        is DomainResult.Ok -> result.value
        is DomainResult.Err -> error("Invalid stored amount cents: $this")
    }
}
