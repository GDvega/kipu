package pe.kipu.core.domain.model

import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency

/**
 * High-level financial plan structure without calculation logic.
 */
data class FinancialPlan(
    val id: EntityId,
    val estimatedMonthlyIncome: Money,
    val fixedExpenses: Money,
    val initialBalance: Money = Money.ZERO,
    val envelopeIds: List<EntityId> = emptyList(),
    val incomeProfile: IncomeProfile = IncomeProfile.FIXED,
    val payFrequency: PayFrequency = PayFrequency.MONTHLY,
    val budgetCycle: BudgetCycle = BudgetCycle.WEEKLY,
    val antSpendingLimit: Money? = null,
    val antSpendingAlertEnabled: Boolean = true,
    val antSpendingAlertPercent: Int = 80,
    val antSpendingTrackedCategoryIds: Set<EntityId> = emptySet(),
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Financial plan id must not be blank"))
        envelopeIds.any { it.isBlank() } ->
            DomainResult.Err(DomainError.InvalidId("Envelope ids must not be blank"))
        antSpendingAlertPercent !in 1..100 ->
            DomainResult.Err(DomainError.InvalidField("Ant-spending alert percent must be between 1 and 100"))
        antSpendingTrackedCategoryIds.any { it.isBlank() } ->
            DomainResult.Err(DomainError.InvalidId("Tracked category ids must not be blank"))
        else -> DomainResult.Ok(Unit)
    }
}
