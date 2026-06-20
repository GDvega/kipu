package pe.kipu.core.domain.model

/**
 * High-level financial plan structure without calculation logic.
 */
data class FinancialPlan(
    val id: EntityId,
    val estimatedMonthlyIncome: Money,
    val fixedExpenses: Money,
    val envelopeIds: List<EntityId> = emptyList(),
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Financial plan id must not be blank"))
        envelopeIds.any { it.isBlank() } ->
            DomainResult.Err(DomainError.InvalidId("Envelope ids must not be blank"))
        else -> DomainResult.Ok(Unit)
    }
}
