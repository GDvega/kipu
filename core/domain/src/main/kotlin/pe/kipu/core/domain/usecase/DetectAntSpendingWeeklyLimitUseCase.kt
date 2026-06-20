package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Money

/**
 * Detects when weekly ant-spending envelope usage crosses the user-configured alert threshold.
 */
class DetectAntSpendingWeeklyLimitUseCase @Inject constructor() {

    operator fun invoke(
        antEnvelopeBudget: EnvelopeBudgetState?,
        alertEnabled: Boolean,
        alertPercent: Int,
        configuredWeeklyLimit: Money?,
    ): AntSpendingWeeklyLimitStatus {
        if (!alertEnabled || antEnvelopeBudget == null) {
            return AntSpendingWeeklyLimitStatus.NotConfigured
        }

        val weeklyLimit = configuredWeeklyLimit ?: antEnvelopeBudget.weeklyLimit
        if (weeklyLimit.isZero()) return AntSpendingWeeklyLimitStatus.NotConfigured

        val percent = alertPercent.coerceIn(1, 100)
        val threshold = weeklyLimit.amount
            .multiply(BigDecimal.valueOf(percent.toLong()))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)

        val thresholdMoney = when (val result = Money.of(threshold)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> return AntSpendingWeeklyLimitStatus.NotConfigured
        }

        val spent = antEnvelopeBudget.spentAmount
        return if (spent.amount >= thresholdMoney.amount) {
            AntSpendingWeeklyLimitStatus.ThresholdReached(
                spentAmount = spent,
                weeklyLimit = weeklyLimit,
                thresholdAmount = thresholdMoney,
                alertPercent = percent,
            )
        } else {
            AntSpendingWeeklyLimitStatus.BelowThreshold(
                spentAmount = spent,
                weeklyLimit = weeklyLimit,
                thresholdAmount = thresholdMoney,
                alertPercent = percent,
            )
        }
    }
}

sealed interface AntSpendingWeeklyLimitStatus {
    data object NotConfigured : AntSpendingWeeklyLimitStatus

    data class BelowThreshold(
        val spentAmount: Money,
        val weeklyLimit: Money,
        val thresholdAmount: Money,
        val alertPercent: Int,
    ) : AntSpendingWeeklyLimitStatus

    data class ThresholdReached(
        val spentAmount: Money,
        val weeklyLimit: Money,
        val thresholdAmount: Money,
        val alertPercent: Int,
    ) : AntSpendingWeeklyLimitStatus
}
