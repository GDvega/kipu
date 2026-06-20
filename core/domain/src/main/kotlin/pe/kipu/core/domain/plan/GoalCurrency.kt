package pe.kipu.core.domain.plan

import java.math.BigDecimal
import java.math.RoundingMode
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money

enum class GoalCurrency(val code: String, val symbol: String) {
    PEN(code = "PEN", symbol = "S/"),
    USD(code = "USD", symbol = "US$"),
}

fun GoalType.currency(): GoalCurrency = when (this) {
    GoalType.DOLLARS -> GoalCurrency.USD
    GoalType.EMERGENCY,
    GoalType.TRAVEL,
    GoalType.PURCHASE,
    GoalType.DEBT,
    -> GoalCurrency.PEN
}

object CurrencyConverter {
    fun toPen(amount: Money, currencyCode: String): Money {
        if (currencyCode == GoalCurrency.USD.code) {
            val converted = amount.amount
                .multiply(PeruPlanDefaults.REFERENCE_USD_TO_PEN)
                .setScale(2, RoundingMode.HALF_UP)
            return when (val result = Money.of(converted)) {
                is DomainResult.Ok -> result.value
                is DomainResult.Err -> Money.ZERO
            }
        }
        return amount
    }
}
