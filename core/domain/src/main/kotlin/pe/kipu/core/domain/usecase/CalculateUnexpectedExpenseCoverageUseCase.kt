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
    ): UnexpectedExpenseCoverage {
        val reserve = reserveBalance.max(BigDecimal.ZERO)
        val available = availableBalance.max(BigDecimal.ZERO)
        val fromReserve = expense.amount.min(reserve)
        val afterReserve = expense.amount - fromReserve
        val fromAvailable = afterReserve.min(available)
        val uncovered = afterReserve - fromAvailable

        return UnexpectedExpenseCoverage(
            fromReserve = Money.of(fromReserve).getOrError(),
            fromAvailableBalance = Money.of(fromAvailable).getOrError(),
            uncovered = Money.of(uncovered).getOrError(),
            isFullyCovered = uncovered.signum() == 0,
        )
    }
}
