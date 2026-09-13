package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.UnexpectedExpenseCoverage
import pe.kipu.core.domain.model.getOrError

class CalculateUnexpectedExpenseCoverageUseCase @Inject constructor() {
    operator fun invoke(
        expense: Money,
        reserveBalance: BigDecimal,
        availableBalance: BigDecimal,
        protectedObligations: Money = Money.ZERO,
    ): UnexpectedExpenseCoverage {
        val reserve = reserveBalance.max(BigDecimal.ZERO)
        // availableBalance is signed net cash minus nominal positive reserve, not free spending money.
        val cash = availableBalance + reserve
        val capacity = (cash - protectedObligations.amount).max(BigDecimal.ZERO)
        val fromReserve = expense.amount.min(reserve).min(capacity)
        val afterReserve = expense.amount - fromReserve
        val fromAvailable = afterReserve.min((capacity - reserve).max(BigDecimal.ZERO))
        val uncovered = afterReserve - fromAvailable
        val existingShortfall = (protectedObligations.amount - cash).max(BigDecimal.ZERO)

        return UnexpectedExpenseCoverage(
            fromReserve = Money.of(fromReserve).getOrError(),
            fromAvailableBalance = Money.of(fromAvailable).getOrError(),
            uncovered = Money.of(uncovered).getOrError(),
            isFullyCovered = uncovered.signum() == 0 && existingShortfall.signum() == 0,
            protectedObligations = protectedObligations,
            liquidityGap = Money.of((expense.amount - cash).max(BigDecimal.ZERO)).getOrError(),
            existingShortfall = Money.of(existingShortfall).getOrError(),
        )
    }
}
