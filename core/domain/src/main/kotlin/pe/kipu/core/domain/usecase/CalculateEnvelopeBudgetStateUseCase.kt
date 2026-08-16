package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.EnvelopeBudgetThresholds
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.time.CycleRange

class CalculateEnvelopeBudgetStateUseCase @Inject constructor(
    private val calculateCategoryPeriodSpent: CalculateCategoryPeriodSpentUseCase,
) {

    operator fun invoke(
        envelope: Envelope,
        movements: List<Movement>,
        cycleRange: CycleRange,
        gatheringLinkedMovementIds: Set<EntityId> = emptySet(),
    ): EnvelopeBudgetState {
        val spentAmount = calculateCategoryPeriodSpent(
            categoryId = envelope.categoryId,
            movements = movements,
            cycleRange = cycleRange,
            gatheringLinkedMovementIds = gatheringLinkedMovementIds,
        )
        val percentUsed = calculatePercentUsed(spentAmount, envelope.weeklyLimit)
        val status = resolveStatus(spentAmount, envelope.weeklyLimit, percentUsed)
        val remainingAmount = calculateRemaining(spentAmount, envelope.weeklyLimit)

        return EnvelopeBudgetState(
            envelopeId = envelope.id,
            name = envelope.name,
            categoryId = envelope.categoryId,
            weeklyLimit = envelope.weeklyLimit,
            spentAmount = spentAmount,
            remainingAmount = remainingAmount,
            percentUsed = percentUsed,
            status = status,
        )
    }

    private fun calculateRemaining(spent: Money, limit: Money): Money {
        if (spent.amount > limit.amount) return Money.ZERO
        return when (val result = limit.minus(spent)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private fun resolveStatus(
        spent: Money,
        limit: Money,
        percentUsed: Int,
    ): EnvelopeBudgetStatus = when {
        spent.amount > limit.amount -> EnvelopeBudgetStatus.EXCEEDED
        percentUsed >= EnvelopeBudgetThresholds.ADJUSTED_PERCENT -> EnvelopeBudgetStatus.ADJUSTED
        else -> EnvelopeBudgetStatus.OK
    }

    companion object {
        fun calculatePercentUsed(spent: Money, limit: Money): Int {
            if (limit.isZero()) return 0
            val percent = spent.amount
                .divide(limit.amount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
            return if (percent > BigDecimal.valueOf(Int.MAX_VALUE.toLong())) {
                Int.MAX_VALUE
            } else {
                percent.intValueExact()
            }
        }
    }
}
