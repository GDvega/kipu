package pe.kipu.core.domain.model

/**
 * UI-oriented snapshot for a commitment; domain keys only (no Spanish strings).
 */
data class CommitmentSummary(
    val commitment: Commitment,
    val savingsProgress: SavingsGoalProgress? = null,
    val statusKey: String,
)
