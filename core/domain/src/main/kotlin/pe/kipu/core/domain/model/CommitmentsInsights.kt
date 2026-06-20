package pe.kipu.core.domain.model

data class CommitmentsInsights(
    val summaries: List<CommitmentSummary>,
    val planValidation: FinancialPlanValidationResult?,
)
