package pe.kipu.core.data.mapper

import java.time.LocalDate
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import java.math.BigDecimal

fun CommitmentEntity.toDomain(): Commitment = Commitment(
    id = id,
    type = CommitmentType.entries.firstOrNull { it.name == type } ?: CommitmentType.SAVINGS_GOAL,
    title = title,
    targetAmount = targetAmountCents?.let(::centsToMoney),
    currentAmount = currentAmountCents?.let(::centsToMoney),
    dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
    counterpartyName = counterpartyName,
    isSettled = isSettled,
    currencyCode = currencyCode,
)

fun Commitment.toEntity(): CommitmentEntity = CommitmentEntity(
    id = id,
    type = type.name,
    title = title,
    targetAmountCents = targetAmount?.let(::moneyToCents),
    currentAmountCents = currentAmount?.let(::moneyToCents),
    dueDateEpochDay = dueDate?.toEpochDay(),
    counterpartyName = counterpartyName,
    isSettled = isSettled,
    currencyCode = currencyCode,
)

private fun moneyToCents(money: Money): Long =
    money.amount.movePointRight(2).longValueExact()

private fun centsToMoney(cents: Long): Money {
    val value = BigDecimal.valueOf(cents).movePointLeft(2)
    return when (val result = Money.of(value)) {
        is DomainResult.Ok -> result.value
        is DomainResult.Err -> error("Invalid stored commitment amount cents: $cents")
    }
}
