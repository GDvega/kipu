package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.AntSpendingAlertKeys
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.HomeInsights

import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveHomeInsightsUseCase @Inject constructor(
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val movementRepository: MovementRepository,
    private val commitmentRepository: CommitmentRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateCycleAvailable: CalculateCycleAvailableUseCase,
    private val detectAntSpending: DetectAntSpendingUseCase,
    private val detectAntSpendingWeeklyLimitUseCase: DetectAntSpendingWeeklyLimitUseCase,
    private val calculateCashFlowSummary: CalculateCashFlowSummaryUseCase,
    private val cycleRangeCalculator: CycleRangeCalculator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<HomeInsights> =
        combine(
            observeEnvelopeBudgets(),
            movementRepository.observeMovements(),
            commitmentRepository.observeCommitments(),
            userPreferencesRepository.observePreferences(),
            timeProvider.refreshTicks(),
        ) { budgets, movements, commitments, preferences, referenceInstant ->
            val cycle = preferences.budgetCycle
            val cycleRange = cycleRangeCalculator.currentCycleRange(cycle, referenceInstant)
            val cycleAvailable = calculateCycleAvailable(
                budgets = budgets,
                referenceInstant = referenceInstant,
                cycleRange = cycleRange,
                cycle = cycle,
            )
            val patternAlerts = detectAntSpending(
                movements = movements,
                referenceInstant = referenceInstant,
                isOverBudget = cycleAvailable.isOverBudget,
                envelopeBudgets = budgets,
            )
            val antEnvelope = budgets.find { it.envelopeId == DefaultPlanEnvelopeIds.ANT_SPENDING }
            val configuredLimit = preferences.antSpendingWeeklyLimitCents?.let(::centsToMoney)
            val weeklyLimitStatus = detectAntSpendingWeeklyLimitUseCase(
                antEnvelopeBudget = antEnvelope,
                alertEnabled = preferences.antSpendingAlertEnabled,
                alertPercent = preferences.antSpendingAlertPercent,
                configuredWeeklyLimit = configuredLimit,
            )
            val weeklyLimitAlert = when (weeklyLimitStatus) {
                is AntSpendingWeeklyLimitStatus.ThresholdReached -> AntSpendingAlert(
                    severity = AlertSeverity.RED,
                    transactionCount = 0,
                    totalAmount = weeklyLimitStatus.spentAmount,
                    windowHours = 0,
                    categoryId = antEnvelope?.categoryId,
                    messageKey = AntSpendingAlertKeys.WEEKLY_LIMIT,
                )

                else -> null
            }
            val antSpendingAlerts = buildList {
                addAll(patternAlerts)
                weeklyLimitAlert?.let(::add)
            }
            val totalCycleLimit = budgets.fold(Money.ZERO) { acc, budget -> acc + budget.cycleLimit }
            val totalCycleSpent = budgets.fold(Money.ZERO) { acc, budget -> acc + budget.spentAmount }
            val periodSummary = if (budgets.isNotEmpty()) {
                pe.kipu.core.domain.model.HomePeriodSummary(
                    totalCycleLimit = totalCycleLimit,
                    totalCycleSpent = totalCycleSpent,
                    daysRemainingInCycle = cycleAvailable.daysRemainingInCycle,
                )
            } else {
                null
            }
            val recentMovements = movements
                .filter { it.status == pe.kipu.core.domain.model.MovementStatus.CONFIRMED }
                .sortedByDescending { it.recordedAt }
                .take(RECENT_MOVEMENTS_LIMIT)
            val cashFlowSummary = calculateCashFlowSummary(movements, commitments)
            HomeInsights(
                cycleAvailable = cycleAvailable,
                antSpendingAlerts = antSpendingAlerts,
                movementCount = movements.size,
                envelopeCount = budgets.size,
                periodSummary = periodSummary,
                recentMovements = recentMovements,
                userPreferences = preferences,
                cashFlowSummary = cashFlowSummary,
            )
        }

    private fun centsToMoney(cents: Long): Money {
        val value = java.math.BigDecimal.valueOf(cents).movePointLeft(2)
        return when (val result = Money.of(value)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private companion object {
        const val RECENT_MOVEMENTS_LIMIT = 3
    }
}
