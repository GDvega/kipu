package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import java.math.BigDecimal

private const val ENVELOPE_ID_SEPARATOR = ","
private const val CATEGORY_ID_SEPARATOR = ","

fun FinancialPlanEntity.toDomain(): FinancialPlan = FinancialPlan(
    id = id,
    estimatedMonthlyIncome = centsToMoney(estimatedMonthlyIncomeCents),
    fixedExpenses = centsToMoney(fixedExpensesCents),
    initialBalance = centsToMoney(initialBalanceCents),
    reserveMonthlyContribution = centsToMoney(reserveMonthlyContributionCents),
    envelopeIds = parseEnvelopeIds(envelopeIds),
    incomeProfile = parseIncomeProfile(incomeProfile),
    payFrequency = parsePayFrequency(payFrequency),
    budgetCycle = parseBudgetCycle(budgetCycle),
    antSpendingLimit = antSpendingLimitCents?.let(::centsToMoney),
    antSpendingAlertEnabled = antSpendingAlertEnabled,
    antSpendingAlertPercent = antSpendingAlertPercent,
    antSpendingTrackedCategoryIds = parseIds(antSpendingTrackedCategoryIds, CATEGORY_ID_SEPARATOR).toSet(),
    electricityExpenses = electricityExpensesCents?.let(::centsToMoney),
    waterExpenses = waterExpensesCents?.let(::centsToMoney),
    internetExpenses = internetExpensesCents?.let(::centsToMoney),
    rentExpenses = rentExpensesCents?.let(::centsToMoney),
    phoneExpenses = phoneExpensesCents?.let(::centsToMoney),
    debtsExpenses = debtsExpensesCents?.let(::centsToMoney),
    educationExpenses = educationExpensesCents?.let(::centsToMoney),
    customFixedExpensesJson = customFixedExpensesJson,
)

fun FinancialPlan.toEntity(): FinancialPlanEntity = FinancialPlanEntity(
    id = id,
    estimatedMonthlyIncomeCents = moneyToCents(estimatedMonthlyIncome),
    fixedExpensesCents = moneyToCents(fixedExpenses),
    initialBalanceCents = moneyToCents(initialBalance),
    reserveMonthlyContributionCents = moneyToCents(reserveMonthlyContribution),
    envelopeIds = envelopeIds.joinToString(ENVELOPE_ID_SEPARATOR),
    incomeProfile = incomeProfile.name,
    payFrequency = payFrequency.name,
    budgetCycle = budgetCycle.name,
    antSpendingLimitCents = antSpendingLimit?.let(::moneyToCents),
    antSpendingAlertEnabled = antSpendingAlertEnabled,
    antSpendingAlertPercent = antSpendingAlertPercent,
    antSpendingTrackedCategoryIds = antSpendingTrackedCategoryIds
        .sorted()
        .joinToString(CATEGORY_ID_SEPARATOR),
    electricityExpensesCents = electricityExpenses?.let(::moneyToCents),
    waterExpensesCents = waterExpenses?.let(::moneyToCents),
    internetExpensesCents = internetExpenses?.let(::moneyToCents),
    rentExpensesCents = rentExpenses?.let(::moneyToCents),
    phoneExpensesCents = phoneExpenses?.let(::moneyToCents),
    debtsExpensesCents = debtsExpenses?.let(::moneyToCents),
    educationExpensesCents = educationExpenses?.let(::moneyToCents),
    customFixedExpensesJson = customFixedExpensesJson,
)

private fun parseIncomeProfile(raw: String): IncomeProfile =
    runCatching { IncomeProfile.valueOf(raw) }.getOrDefault(IncomeProfile.FIXED)

private fun parsePayFrequency(raw: String): PayFrequency =
    runCatching { PayFrequency.valueOf(raw) }.getOrDefault(PayFrequency.MONTHLY)

private fun parseBudgetCycle(raw: String): pe.kipu.core.domain.model.BudgetCycle =
    runCatching { pe.kipu.core.domain.model.BudgetCycle.valueOf(raw) }.getOrDefault(pe.kipu.core.domain.model.BudgetCycle.WEEKLY)


private fun parseEnvelopeIds(raw: String): List<String> = parseIds(raw, ENVELOPE_ID_SEPARATOR)

private fun parseIds(raw: String, separator: String): List<String> =
    if (raw.isBlank()) {
        emptyList()
    } else {
        raw.split(separator).map { it.trim() }.filter { it.isNotEmpty() }
    }

private fun moneyToCents(money: Money): Long =
    money.amount.movePointRight(2).longValueExact()

private fun centsToMoney(cents: Long): Money {
    val value = BigDecimal.valueOf(cents).movePointLeft(2)
    return when (val result = Money.of(value)) {
        is DomainResult.Ok -> result.value
        is DomainResult.Err -> error("Invalid stored financial plan amount cents")
    }
}
