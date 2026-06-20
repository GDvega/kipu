package pe.kipu.core.domain.model

/**
 * Progress snapshot for a [CommitmentType.SAVINGS_GOAL].
 */
data class SavingsGoalProgress(
    val commitmentId: EntityId,
    val savedAmount: Money,
    val targetAmount: Money,
    /** Progress percentage capped at 100. */
    val progressPercent: Int,
    val isCompleted: Boolean,
)
