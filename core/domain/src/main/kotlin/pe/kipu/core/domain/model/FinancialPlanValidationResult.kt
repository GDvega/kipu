package pe.kipu.core.domain.model

sealed interface FinancialPlanValidationResult {
    data object Valid : FinancialPlanValidationResult

    data class Invalid(val deficit: Money) : FinancialPlanValidationResult
}
