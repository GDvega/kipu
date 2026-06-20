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
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveHomeInsightsUseCase @Inject constructor(
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val movementRepository: MovementRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateDailyAvailable: CalculateDailyAvailableUseCase,
    private val detectAntSpending: DetectAntSpendingUseCase,
    private val detectAntSpendingWeeklyLimitUseCase: DetectAntSpendingWeeklyLimitUseCase,
    private val weekRangeCalculator: WeekRangeCalculator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<HomeInsights> =
        combine(
            observeEnvelopeBudgets(),
            movementRepository.observeMovements(),
            userPreferencesRepository.observePreferences(),
            timeProvider.refreshTicks(),
        ) { budgets, movements, preferences, referenceInstant ->
            val weekRange = weekRangeCalculator.currentWeekRange(referenceInstant)
            val dailyAvailable = calculateDailyAvailable(
                budgets = budgets,
                referenceInstant = referenceInstant,
                weekRange = weekRange,
            )
            val patternAlerts = detectAntSpending(
                movements = movements,
                referenceInstant = referenceInstant,
                isOverBudget = dailyAvailable.isOverBudget,
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
            HomeInsights(
                dailyAvailable = dailyAvailable,
                antSpendingAlerts = antSpendingAlerts,
                movementCount = movements.size,
                envelopeCount = budgets.size,
            )
        }

    private fun centsToMoney(cents: Long): Money {
        val value = java.math.BigDecimal.valueOf(cents).movePointLeft(2)
        return when (val result = Money.of(value)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }
}
