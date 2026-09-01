package pe.kipu.core.domain.model

import java.math.BigDecimal

/**
 * Monthly plan arithmetic used by validation and wizard summary.
 */
data class FinancialPlanBreakdown(
    val monthlyEnvelopeReserve: Money,
    val reserveMonthlyContribution: Money,
    val commitmentsBurden: Money,
    val totalOutflows: Money,
    /** Signed balance: estimated income minus total outflows (may be negative). */
    val monthlySurplus: BigDecimal,
    val validation: FinancialPlanValidationResult,
)
