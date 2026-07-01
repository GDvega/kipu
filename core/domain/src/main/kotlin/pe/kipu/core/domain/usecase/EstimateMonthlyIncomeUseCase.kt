package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.plan.PlanWizardLineItem
import pe.kipu.core.domain.util.MoneyInputParser

/**
 * Converts wizard income inputs into a single monthly estimate.
 *
 * Supuestos MVP:
 * - Ingresos extras en perfil fijo se suman al mensual (no se prorratean).
 * - Quincenal: mensual = 1ra quincena + 2da quincena (sin multiplicar).
 * - Ingreso variable usa promedio de semana baja/normal/buena × 4 semanas.
 */
class EstimateMonthlyIncomeUseCase @Inject constructor() {

    fun fromFixed(
        baseAmountText: String,
        frequency: PayFrequency,
        secondQuincenaText: String = "",
        extraIncomeText: String = "",
        additionalLines: List<PlanWizardLineItem> = emptyList(),
    ): DomainResult<Money> {
        val extrasFromLegacy = parseOptional(extraIncomeText) ?: return invalidAmount()
        val extrasFromLines = sumAdditionalLines(additionalLines) ?: return invalidAmount()
        val totalExtras = extrasFromLegacy.amount.add(extrasFromLines)

        val monthlyBase = when (frequency) {
            PayFrequency.MONTHLY -> {
                val base = parseOptional(baseAmountText) ?: return invalidAmount()
                base.amount
            }
            PayFrequency.BIWEEKLY -> {
                val first = parseOptional(baseAmountText) ?: return invalidAmount()
                val second = parseOptional(secondQuincenaText) ?: return invalidAmount()
                if (first.isZero() && second.isZero()) return invalidAmount()
                first.amount.add(second.amount)
            }
            PayFrequency.WEEKLY -> {
                val base = parseOptional(baseAmountText) ?: return invalidAmount()
                if (base.isZero()) return invalidAmount()
                base.amount.multiply(WEEKLY_TO_MONTHLY)
            }
        }

        if (frequency != PayFrequency.BIWEEKLY) {
            val base = parseOptional(baseAmountText) ?: return invalidAmount()
            if (base.isZero() && totalExtras == BigDecimal.ZERO) return invalidAmount()
        }

        return Money.of(monthlyBase.add(totalExtras).setScale(2, RoundingMode.HALF_UP))
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
        secondQuincenaText: String = "",
        extraIncomeText: String = "",
        additionalIncomeLines: List<PlanWizardLineItem> = emptyList(),
        lowWeekText: String = "",
        normalWeekText: String = "",
        goodWeekText: String = "",
        approximateText: String = "",
    ): DomainResult<Money> = when (profile) {
        IncomeProfile.FIXED -> fromFixed(
            baseAmountText = fixedBaseText,
            frequency = frequency,
            secondQuincenaText = secondQuincenaText,
            extraIncomeText = extraIncomeText,
            additionalLines = additionalIncomeLines,
        )
        IncomeProfile.VARIABLE -> fromVariable(lowWeekText, normalWeekText, goodWeekText)
        IncomeProfile.APPROXIMATE -> fromApproximate(approximateText)
    }

    private fun sumAdditionalLines(lines: List<PlanWizardLineItem>): BigDecimal? {
        var total = BigDecimal.ZERO
        for (line in lines) {
            val parsed = parseOptional(line.amountText) ?: return null
            total = total.add(parsed.amount)
        }
        return total
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
        val WEEKLY_TO_MONTHLY: BigDecimal = BigDecimal("4")
        val AVERAGE_DIVISOR: BigDecimal = BigDecimal("3")
    }
}
