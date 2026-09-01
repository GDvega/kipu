package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.entity.MonthlyServiceReceiptEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import java.math.BigDecimal
import java.time.Instant

fun MonthlyServiceReceiptEntity.toDomain(): MonthlyServiceReceipt = MonthlyServiceReceipt(
    key = ServiceReceiptKey.fromIdentifier(serviceKeyIdentifier),
    title = title,
    configuredAmount = centsToMoney(configuredAmountCents),
    monthKey = monthKey,
    isPaid = isPaid,
    paidMovementId = paidMovementId,
    paidAt = paidAtEpochMs?.let(Instant::ofEpochMilli),
)

fun MonthlyServiceReceipt.toEntity(): MonthlyServiceReceiptEntity = MonthlyServiceReceiptEntity(
    id = "${monthKey}_${key.identifier}",
    monthKey = monthKey,
    serviceKeyIdentifier = key.identifier,
    title = title,
    configuredAmountCents = moneyToCents(configuredAmount),
    isPaid = isPaid,
    paidMovementId = paidMovementId,
    paidAtEpochMs = paidAt?.toEpochMilli(),
)

private fun moneyToCents(money: Money): Long =
    money.amount.movePointRight(2).longValueExact()

private fun centsToMoney(cents: Long): Money {
    val value = BigDecimal.valueOf(cents).movePointLeft(2)
    return when (val result = Money.of(value)) {
        is DomainResult.Ok -> result.value
        is DomainResult.Err -> error("Invalid stored receipt amount cents")
    }
}
