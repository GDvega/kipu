package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.CurrencyConverter

/**
 * Validates whether estimated monthly income covers planned outflows.
 *
 * MVP formula:
 * - monthlyEnvelopeReserve = sum(weeklyLimit) * 4 for plan envelopes (or all if [FinancialPlan.envelopeIds] empty)
 * - commitmentsBurden = remaining savings targets + unsettled social debts / pending payments
 * - totalOutflows = fixedExpenses + monthlyEnvelopeReserve + commitmentsBurden
 * - deficit = totalOutflows - estimatedMonthlyIncome when income is insufficient
 */
class ValidateFinancialPlanUseCase @Inject constructor() {

    operator fun invoke(
        plan: FinancialPlan,
        envelopes: List<Envelope>,
        commitments: List<Commitment>,
    ): FinancialPlanValidationResult {
        val relevantEnvelopes = if (plan.envelopeIds.isEmpty()) {
            envelopes
        } else {
            envelopes.filter { envelope -> envelope.id in plan.envelopeIds }
        }

        val weeklyEnvelopeTotal = relevantEnvelopes.fold(Money.ZERO) { acc, envelope ->
            acc + envelope.weeklyLimit
        }
        val monthlyEnvelopeReserve = multiplyMoney(weeklyEnvelopeTotal, WEEKS_PER_MONTH)
        val commitmentsBurden = commitments.fold(Money.ZERO) { acc, commitment ->
            acc + commitmentBurden(commitment)
        }
        val totalOutflows = plan.fixedExpenses + monthlyEnvelopeReserve + commitmentsBurden

        if (plan.estimatedMonthlyIncome.isZero()) {
            return FinancialPlanValidationResult.Invalid(deficit = totalOutflows)
        }

        return if (plan.estimatedMonthlyIncome.amount >= totalOutflows.amount) {
            FinancialPlanValidationResult.Valid
        } else {
            val deficit = subtractMoney(totalOutflows, plan.estimatedMonthlyIncome)
            FinancialPlanValidationResult.Invalid(deficit = deficit)
        }
    }

    private fun commitmentBurden(commitment: Commitment): Money {
        if (commitment.isSettled) return Money.ZERO

        return when (commitment.type) {
            CommitmentType.SAVINGS_GOAL -> {
                val target = commitment.targetAmount ?: return Money.ZERO
                val current = commitment.currentAmount ?: Money.ZERO
                val remaining = subtractMoney(target, current)
                CurrencyConverter.toPen(remaining, commitment.currencyCode)
            }

            CommitmentType.SOCIAL_DEBT,
            CommitmentType.PENDING_PAYMENT,
            -> commitment.currentAmount ?: Money.ZERO
        }
    }

    private fun multiplyMoney(amount: Money, factor: Int): Money {
        val product = amount.amount.multiply(BigDecimal.valueOf(factor.toLong()))
        return when (val result = Money.of(product)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private fun subtractMoney(minuend: Money, subtrahend: Money): Money =
        when (val result = minuend.minus(subtrahend)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }

    private companion object {
        const val WEEKS_PER_MONTH: Int = 4
    }
}
