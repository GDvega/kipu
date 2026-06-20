package pe.kipu.core.domain

import java.math.BigDecimal

/**
 * Heuristic thresholds for ant spending (gastos hormiga) detection.
 *
 * [MAX_SINGLE_AMOUNT]: expenses at or below this amount are considered "small".
 * Supuesto MVP: S/ 20.00 — typical snack / micro-purchase in Peru.
 */
object AntSpendingThresholds {
    val MAX_SINGLE_AMOUNT: BigDecimal = BigDecimal("20.00")
    const val MIN_TRANSACTION_COUNT: Int = 3
    const val WINDOW_HOURS: Int = 48
}
