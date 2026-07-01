package pe.kipu.core.domain.model

/**
 * Calculated weekly budget snapshot for an envelope.
 * [spentAmount] is derived from movements; never persisted in Room.
 */
data class EnvelopeBudgetState(
    val envelopeId: EntityId,
    val name: String,
    val categoryId: EntityId,
    val weeklyLimit: Money,
    val spentAmount: Money,
    val remainingAmount: Money,
    val percentUsed: Int,
    val status: EnvelopeBudgetStatus,
) {
    val cycleLimit: Money get() = weeklyLimit
}
