package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.AvailableBalance
import pe.kipu.core.domain.model.CashFlowSummary
import pe.kipu.core.domain.model.ReserveBalance

class CalculateAvailableBalanceUseCase @Inject constructor() {
    operator fun invoke(
        cashFlow: CashFlowSummary,
        reserve: ReserveBalance,
    ): AvailableBalance {
        val protectedReserve = reserve.balance.max(BigDecimal.ZERO)
        val available = cashFlow.netCash - protectedReserve
        return AvailableBalance(
            netCash = cashFlow.netCash,
            reserveBalance = reserve.balance,
            availableBalance = available.setScale(2),
            isOverdrawn = available.signum() < 0,
        )
    }
}
