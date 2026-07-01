package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.SavingsGoalProgress

class CalculateSavingsGoalProgressUseCase @Inject constructor() {

    operator fun invoke(
        commitment: Commitment,
        linkedIncome: Money = Money.ZERO,
    ): DomainResult<SavingsGoalProgress> {
        if (commitment.type != CommitmentType.SAVINGS_GOAL) {
            return DomainResult.Err(
                DomainError.InvalidField("Savings goal progress requires SAVINGS_GOAL commitment"),
            )
        }

        val targetAmount = commitment.targetAmount
            ?: return DomainResult.Err(DomainError.InvalidField("Savings goal requires a target amount"))
        if (targetAmount.isZero() || targetAmount.amount <= BigDecimal.ZERO) {
            return DomainResult.Err(DomainError.InvalidAmount("Savings goal target must be greater than zero"))
        }

        val savedAmount = (commitment.currentAmount ?: Money.ZERO) + linkedIncome
        val isCompleted = commitment.isSettled || savedAmount.amount >= targetAmount.amount
        val progressPercent = calculateProgressPercent(savedAmount, targetAmount)

        return DomainResult.Ok(
            SavingsGoalProgress(
                commitmentId = commitment.id,
                savedAmount = savedAmount,
                targetAmount = targetAmount,
                progressPercent = progressPercent,
                isCompleted = isCompleted,
            ),
        )
    }

    private fun calculateProgressPercent(savedAmount: Money, targetAmount: Money): Int {
        val ratio = savedAmount.amount
            .divide(targetAmount.amount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
        return ratio.setScale(0, RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, 100)
    }
}
