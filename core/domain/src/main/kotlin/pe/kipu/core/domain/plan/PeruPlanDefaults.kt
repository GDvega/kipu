package pe.kipu.core.domain.plan

import java.math.BigDecimal

/**
 * Reference amounts for Peru (RMV 2025–2026, D.S. 006-2024-TR) and Kipu seed defaults.
 * Display-only anchors; user always confirms before save.
 */
object PeruPlanDefaults {
    val RMV_MONTHLY: BigDecimal = BigDecimal("1130")
    val TYPICAL_INFORMAL_MONTHLY: BigDecimal = BigDecimal("1800")
    val TYPICAL_EMPLOYEE_MONTHLY: BigDecimal = BigDecimal("3000")

    val SEED_INCOME_MONTHLY: BigDecimal = TYPICAL_EMPLOYEE_MONTHLY
    val SEED_FIXED_EXPENSES_MONTHLY: BigDecimal = BigDecimal("1800")

    val SEED_EDUCATION_MONTHLY: BigDecimal = BigDecimal.ZERO
    val SEED_RENT_MONTHLY: BigDecimal = BigDecimal("900")
    val SEED_UTILITIES_MONTHLY: BigDecimal = BigDecimal("350")
    val SEED_PHONE_MONTHLY: BigDecimal = BigDecimal("50")
    val SEED_DEBTS_MONTHLY: BigDecimal = BigDecimal("550")

    /** Tipo de cambio referencial solo para validar metas en USD dentro del plan en soles. */
    val REFERENCE_USD_TO_PEN: BigDecimal = BigDecimal("3.75")
}

enum class IncomeTemplate {
    RMV,
    INFORMAL,
    EMPLOYEE,
}

fun IncomeTemplate.monthlyAmount(): BigDecimal = when (this) {
    IncomeTemplate.RMV -> PeruPlanDefaults.RMV_MONTHLY
    IncomeTemplate.INFORMAL -> PeruPlanDefaults.TYPICAL_INFORMAL_MONTHLY
    IncomeTemplate.EMPLOYEE -> PeruPlanDefaults.TYPICAL_EMPLOYEE_MONTHLY
}

fun IncomeTemplate.label(): String = when (this) {
    IncomeTemplate.RMV -> "RMV S/ 1,130"
    IncomeTemplate.INFORMAL -> "Independiente S/ 1,800"
    IncomeTemplate.EMPLOYEE -> "Empleado S/ 3,000"
}
