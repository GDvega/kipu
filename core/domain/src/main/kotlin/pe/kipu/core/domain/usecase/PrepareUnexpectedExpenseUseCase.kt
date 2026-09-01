package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.UnexpectedExpensePreview

class PrepareUnexpectedExpenseUseCase @Inject constructor(
    private val observeHomeInsights: ObserveHomeInsightsUseCase,
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val calculateCoverage: CalculateUnexpectedExpenseCoverageUseCase,
    private val buildRecoveryPlan: BuildUnexpectedExpenseRecoveryPlanUseCase,
) {
    suspend operator fun invoke(expense: Money): UnexpectedExpensePreview {
        val insights = observeHomeInsights().first()
        val coverage = calculateCoverage(
            expense = expense,
            reserveBalance = insights.reserveBalance?.balance ?: java.math.BigDecimal.ZERO,
            availableBalance = insights.availableBalance?.availableBalance ?: java.math.BigDecimal.ZERO,
        )
        return UnexpectedExpensePreview(
            coverage = coverage,
            recoveryPlan = buildRecoveryPlan(
                uncovered = coverage.uncovered,
                budgets = observeEnvelopeBudgets().first(),
            ),
        )
    }
}
