package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.util.MoneyInputParser

/**
 * Converts wizard income inputs into a single monthly estimate.
 *
 * Supuestos MVP:
 * - Ingresos extras en perfil fijo se suman al mensual (no se prorratean).
 * - Ingreso variable usa promedio de semana baja/normal/buena × 4 semanas.
 */
class EstimateMonthlyIncomeUseCase @Inject constructor() {

    fun fromFixed(
        baseAmountText: String,
        frequency: PayFrequency,
        extraIncomeText: String,
    ): DomainResult<Money> {
        val base = parseOptional(baseAmountText) ?: return invalidAmount()
        val extras = parseOptional(extraIncomeText) ?: return invalidAmount()

        val monthlyBase = when (frequency) {
            PayFrequency.MONTHLY -> base.amount
            PayFrequency.BIWEEKLY -> base.amount.multiply(BIWEEKLY_TO_MONTHLY)
            PayFrequency.WEEKLY -> base.amount.multiply(WEEKLY_TO_MONTHLY)
        }

        return Money.of(monthlyBase.add(extras.amount).setScale(2, RoundingMode.HALF_UP))
    }

    fun fromVariable(
        lowWeekText: String,
        normalWeekText: String,
        goodWeekText: String,
    ): DomainResult<Money> {
        val low = parseOptional(lowWeekText) ?: return invalidAmount()
        val normal = parseOptional(normalWeekText) ?: return invalidAmount()
        val good = parseOptional(goodWeekText) ?: return invalidAmount()

        if (low.isZero() && normal.isZero() && good.isZero()) {
            return invalidAmount()
        }

        val averageWeek = low.amount
            .add(normal.amount)
            .add(good.amount)
            .divide(AVERAGE_DIVISOR, 10, RoundingMode.HALF_UP)

        return Money.of(averageWeek.multiply(WEEKLY_TO_MONTHLY).setScale(2, RoundingMode.HALF_UP))
    }

    fun fromApproximate(amountText: String): DomainResult<Money> {
        val amount = parseOptional(amountText) ?: return invalidAmount()
        if (amount.isZero()) return invalidAmount()
        return DomainResult.Ok(amount)
    }

    fun estimate(
        profile: IncomeProfile,
        fixedBaseText: String,
        frequency: PayFrequency,
        extraIncomeText: String,
        lowWeekText: String,
        normalWeekText: String,
        goodWeekText: String,
        approximateText: String,
    ): DomainResult<Money> = when (profile) {
        IncomeProfile.FIXED -> fromFixed(fixedBaseText, frequency, extraIncomeText)
        IncomeProfile.VARIABLE -> fromVariable(lowWeekText, normalWeekText, goodWeekText)
        IncomeProfile.APPROXIMATE -> fromApproximate(approximateText)
    }

    private fun parseOptional(text: String): Money? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Money.ZERO
        return when (val result = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    private fun invalidAmount(): DomainResult.Err =
        DomainResult.Err(pe.kipu.core.domain.model.DomainError.InvalidAmount("Invalid income amount"))

    private companion object {
        val BIWEEKLY_TO_MONTHLY: BigDecimal = BigDecimal("2")
        val WEEKLY_TO_MONTHLY: BigDecimal = BigDecimal("4")
        val AVERAGE_DIVISOR: BigDecimal = BigDecimal("3")
    }
}
