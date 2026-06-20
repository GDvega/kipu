package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money

class CalculateGatheringEqualSplitUseCase @Inject constructor() {

    operator fun invoke(total: Money, participantCount: Int): DomainResult<Money> = when {
        participantCount < 1 ->
            DomainResult.Err(
                pe.kipu.core.domain.model.DomainError.InvalidField("Participant count must be at least 1"),
            )
        total.isZero() -> DomainResult.Ok(Money.ZERO)
        else -> {
            val quotient = total.amount
                .divide(BigDecimal(participantCount), SCALE, RoundingMode.HALF_UP)
            Money.of(quotient)
        }
    }

    private companion object {
        const val SCALE = 2
    }
}
