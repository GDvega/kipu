package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import java.math.BigDecimal

private const val ENVELOPE_ID_SEPARATOR = ","

fun FinancialPlanEntity.toDomain(): FinancialPlan = FinancialPlan(
    id = id,
    estimatedMonthlyIncome = centsToMoney(estimatedMonthlyIncomeCents),
    fixedExpenses = centsToMoney(fixedExpensesCents),
    envelopeIds = parseEnvelopeIds(envelopeIds),
)

fun FinancialPlan.toEntity(): FinancialPlanEntity = FinancialPlanEntity(
    id = id,
    estimatedMonthlyIncomeCents = moneyToCents(estimatedMonthlyIncome),
    fixedExpensesCents = moneyToCents(fixedExpenses),
    envelopeIds = envelopeIds.joinToString(ENVELOPE_ID_SEPARATOR),
)

private fun parseEnvelopeIds(raw: String): List<String> =
    if (raw.isBlank()) {
        emptyList()
    } else {
        raw.split(ENVELOPE_ID_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

private fun moneyToCents(money: Money): Long =
    money.amount.movePointRight(2).longValueExact()

private fun centsToMoney(cents: Long): Money {
    val value = BigDecimal.valueOf(cents).movePointLeft(2)
    return when (val result = Money.of(value)) {
        is DomainResult.Ok -> result.value
        is DomainResult.Err -> error("Invalid stored financial plan amount cents: $cents")
    }
}
