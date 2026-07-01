package pe.kipu.core.domain.plan

import java.math.BigDecimal

/**
 * Reference amounts for Peru and Kipu seed defaults.
 * Display-only anchors; user always confirms before save.
 */
object PeruPlanDefaults {
    val TYPICAL_EMPLOYEE_MONTHLY: BigDecimal = BigDecimal("3000")

    val SEED_INCOME_MONTHLY: BigDecimal = TYPICAL_EMPLOYEE_MONTHLY
    val SEED_FIXED_EXPENSES_MONTHLY: BigDecimal = BigDecimal("1800")

    val SEED_EDUCATION_MONTHLY: BigDecimal = BigDecimal.ZERO
    val SEED_RENT_MONTHLY: BigDecimal = BigDecimal("900")
    val SEED_UTILITIES_MONTHLY: BigDecimal = BigDecimal("350")
    val SEED_PHONE_MONTHLY: BigDecimal = BigDecimal("50")
    val SEED_DEBTS_MONTHLY: BigDecimal = BigDecimal("550")

    val SEED_BIWEEKLY_FIRST: BigDecimal = BigDecimal("750")
    val SEED_BIWEEKLY_SECOND: BigDecimal = BigDecimal("800")
    val SEED_WEEKLY_FIXED: BigDecimal = BigDecimal("400")
    val SEED_MONTHLY_FIXED: BigDecimal = BigDecimal("1500")

    val SEED_VARIABLE_LOW_WEEK: BigDecimal = BigDecimal("250")
    val SEED_VARIABLE_NORMAL_WEEK: BigDecimal = BigDecimal("400")
    val SEED_VARIABLE_GOOD_WEEK: BigDecimal = BigDecimal("650")

    val SEED_GOAL_TARGET: BigDecimal = BigDecimal("1000")
    val SEED_GOAL_CURRENT: BigDecimal = BigDecimal("150")

    /** Tipo de cambio referencial solo para validar metas en USD dentro del plan en soles. */
    val REFERENCE_USD_TO_PEN: BigDecimal = BigDecimal("3.75")
}
