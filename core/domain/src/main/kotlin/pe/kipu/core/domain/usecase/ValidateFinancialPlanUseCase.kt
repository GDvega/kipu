package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanBreakdown
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.SavingsGoalBurdenCalculator

/**
 * Validates whether estimated monthly income covers planned outflows.
 *
 * MVP formula:
 * - monthlyEnvelopeReserve = sum(cycleLimit) proyectado a mes según [FinancialPlan.budgetCycle]
 *   (×30 diario, ×4 semanal, ×1 mensual) para los sobres del plan (o todos si [FinancialPlan.envelopeIds] vacío)
 * - commitmentsBurden = monthly savings quotas for goals + unsettled social debts / pending payments
 * - totalOutflows = fixedExpenses + monthlyEnvelopeReserve + commitmentsBurden
 * - deficit = totalOutflows - estimatedMonthlyIncome when income is insufficient
 */
class ValidateFinancialPlanUseCase @Inject constructor() {

    operator fun invoke(
        plan: FinancialPlan,
        envelopes: List<Envelope>,
        commitments: List<Commitment>,
        linkedIncomeByCommitmentId: Map<EntityId, Money> = emptyMap(),
    ): FinancialPlanValidationResult = analyze(plan, envelopes, commitments, linkedIncomeByCommitmentId).validation

    fun analyze(
        plan: FinancialPlan,
        envelopes: List<Envelope>,
        commitments: List<Commitment>,
        linkedIncomeByCommitmentId: Map<EntityId, Money> = emptyMap(),
    ): FinancialPlanBreakdown {
        val relevantEnvelopes = if (plan.envelopeIds.isEmpty()) {
            envelopes
        } else {
            envelopes.filter { envelope -> envelope.id in plan.envelopeIds }
        }

        val cycleEnvelopeTotal = relevantEnvelopes.fold(Money.ZERO) { acc, envelope ->
            acc + envelope.cycleLimit
        }
        val monthlyEnvelopeReserve = multiplyMoney(cycleEnvelopeTotal, monthsFactor(plan.budgetCycle))
        val commitmentsBurden = commitments.fold(Money.ZERO) { acc, commitment ->
            acc + commitmentBurden(commitment, linkedIncomeByCommitmentId)
        }
        val totalOutflows = plan.fixedExpenses + monthlyEnvelopeReserve + commitmentsBurden

        if (plan.estimatedMonthlyIncome.isZero()) {
            return FinancialPlanBreakdown(
                monthlyEnvelopeReserve = monthlyEnvelopeReserve,
                commitmentsBurden = commitmentsBurden,
                totalOutflows = totalOutflows,
                monthlySurplus = plan.estimatedMonthlyIncome.amount.subtract(totalOutflows.amount),
                validation = FinancialPlanValidationResult.Invalid(deficit = totalOutflows),
            )
        }

        val monthlySurplus = plan.estimatedMonthlyIncome.amount.subtract(totalOutflows.amount)
        val validation = if (plan.estimatedMonthlyIncome.amount >= totalOutflows.amount) {
            FinancialPlanValidationResult.Valid
        } else {
            FinancialPlanValidationResult.Invalid(deficit = subtractMoney(totalOutflows, plan.estimatedMonthlyIncome))
        }

        return FinancialPlanBreakdown(
            monthlyEnvelopeReserve = monthlyEnvelopeReserve,
            commitmentsBurden = commitmentsBurden,
            totalOutflows = totalOutflows,
            monthlySurplus = monthlySurplus,
            validation = validation,
        )
    }

    private fun commitmentBurden(
        commitment: Commitment,
        linkedIncomeByCommitmentId: Map<EntityId, Money>,
    ): Money {
        if (commitment.isSettled) return Money.ZERO

        return when (commitment.type) {
            CommitmentType.SAVINGS_GOAL -> {
                val target = commitment.targetAmount ?: return Money.ZERO
                val linkedIncome = linkedIncomeByCommitmentId[commitment.id] ?: Money.ZERO
                val current = (commitment.currentAmount ?: Money.ZERO) + linkedIncome
                SavingsGoalBurdenCalculator.monthlyBurden(
                    target = target,
                    current = current,
                    horizonMonths = commitment.savingsHorizonMonths,
                    currencyCode = commitment.currencyCode,
                )
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

    /** Factor para proyectar el límite por ciclo a un total mensual. */
    private fun monthsFactor(cycle: BudgetCycle): Int = when (cycle) {
        BudgetCycle.DAILY -> DAYS_PER_MONTH
        BudgetCycle.WEEKLY -> WEEKS_PER_MONTH
        BudgetCycle.MONTHLY -> 1
    }

    private companion object {
        const val WEEKS_PER_MONTH: Int = 4
        const val DAYS_PER_MONTH: Int = 30
    }
}
