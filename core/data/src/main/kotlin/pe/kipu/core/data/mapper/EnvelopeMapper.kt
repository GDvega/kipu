package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.entity.EnvelopeEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Money
import java.math.BigDecimal

fun EnvelopeEntity.toDomain(): Envelope {
    val weeklyLimit = centsToMoney(weeklyLimitCents)
    return Envelope(
        id = id,
        name = name,
        weeklyLimit = weeklyLimit,
        categoryId = categoryId,
        spentAmount = Money.ZERO,
    )
}

fun Envelope.toEntity(): EnvelopeEntity = EnvelopeEntity(
    id = id,
    name = name,
    weeklyLimitCents = moneyToCents(weeklyLimit),
    categoryId = categoryId,
)

private fun moneyToCents(money: Money): Long =
    money.amount.movePointRight(2).longValueExact()

private fun centsToMoney(cents: Long): Money {
    val value = BigDecimal.valueOf(cents).movePointLeft(2)
    return when (val result = Money.of(value)) {
        is DomainResult.Ok -> result.value
        is DomainResult.Err -> error("Invalid stored weekly limit cents: $cents")
    }
}
