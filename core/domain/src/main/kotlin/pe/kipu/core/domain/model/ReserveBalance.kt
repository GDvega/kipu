package pe.kipu.core.domain.model

import java.math.BigDecimal

data class ReserveBalance(
    val totalAdded: Money,
    val totalUsed: Money,
    /** Signed to expose inconsistent or overdrawn data instead of hiding it. */
    val balance: BigDecimal,
    val isOverdrawn: Boolean,
)

data class AvailableBalance(
    val netCash: BigDecimal,
    val reserveBalance: BigDecimal,
    val availableBalance: BigDecimal,
    val isOverdrawn: Boolean,
)

data class UnexpectedExpenseCoverage(
    val fromReserve: Money,
    val fromAvailableBalance: Money,
    val uncovered: Money,
    val isFullyCovered: Boolean,
)
