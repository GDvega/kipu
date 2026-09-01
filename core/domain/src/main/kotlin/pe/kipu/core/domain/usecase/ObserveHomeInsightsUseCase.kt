package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.AntSpendingAlertKeys
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.HomeInsights

import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MonthlyBudgetSummary
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveHomeInsightsUseCase @Inject constructor(
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val movementRepository: MovementRepository,
    private val commitmentRepository: CommitmentRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val reserveEventRepository: ReserveEventRepository,
    private val calculateCycleAvailable: CalculateCycleAvailableUseCase,
    private val detectAntSpending: DetectAntSpendingUseCase,
    private val detectAntSpendingWeeklyLimitUseCase: DetectAntSpendingWeeklyLimitUseCase,
    private val calculateCashFlowSummary: CalculateCashFlowSummaryUseCase,
    private val calculateCategoryExpenseDistribution: CalculateCategoryExpenseDistributionUseCase,
    private val calculateReserveBalance: CalculateReserveBalanceUseCase,
    private val calculateAvailableBalance: CalculateAvailableBalanceUseCase,
    private val cycleRangeCalculator: CycleRangeCalculator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<HomeInsights> =
        combine(
            observeEnvelopeBudgets(),
            movementRepository.observeMovements(),
            commitmentRepository.observeCommitments(),
            categoryRepository.observeCategories(),
            financialPlanRepository.observePlans(),
        ) { budgets, movements, commitments, categories, plans ->
            HomeInputs(
                budgets = budgets,
                movements = movements,
                commitments = commitments,
                categories = categories,
                plan = plans.firstOrNull(),
            )
        }.combine(reserveEventRepository.observeAll()) { inputs, reserveEvents ->
            inputs.copy(reserveEvents = reserveEvents)
        }.combine(userPreferencesRepository.observePreferences()) { inputs, preferences ->
            inputs.copy(preferences = preferences)
        }.combine(timeProvider.refreshTicks()) { inputs, referenceInstant ->
            val budgets = inputs.budgets
            val movements = inputs.movements
            val commitments = inputs.commitments
            val categories = inputs.categories
            val preferences = inputs.preferences ?: pe.kipu.core.domain.model.UserPreferences()
            val plan = inputs.plan
            val cycle = plan?.budgetCycle ?: BudgetCycle.WEEKLY
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
            val configuredLimit = plan?.antSpendingLimit
            val weeklyLimitStatus = detectAntSpendingWeeklyLimitUseCase(
                antEnvelopeBudget = antEnvelope,
                alertEnabled = plan?.antSpendingAlertEnabled ?: true,
                alertPercent = plan?.antSpendingAlertPercent ?: DEFAULT_ALERT_PERCENT,
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
            val cashFlowSummary = calculateCashFlowSummary(
                movements = movements,
                commitments = commitments,
                initialBalance = plan?.initialBalance ?: Money.ZERO,
            )
            val reserveBalance = calculateReserveBalance(inputs.reserveEvents)
            val availableBalance = calculateAvailableBalance(cashFlowSummary, reserveBalance)
            val categoryDistribution = calculateCategoryExpenseDistribution(
                movements = movements,
                categories = categories,
                cycleRange = cycleRange,
            )
            val monthlyBudgetSummary = calculateMonthlyBudgetSummary(
                plan = plan,
                movements = movements,
                referenceInstant = referenceInstant,
            )
            HomeInsights(
                cycleAvailable = cycleAvailable,
                antSpendingAlerts = antSpendingAlerts,
                movementCount = movements.size,
                envelopeCount = budgets.size,
                periodSummary = periodSummary,
                recentMovements = recentMovements,
                userPreferences = preferences,
                cashFlowSummary = cashFlowSummary,
                financialPlan = plan,
                categoryDistribution = categoryDistribution,
                monthlyBudgetSummary = monthlyBudgetSummary,
                reserveBalance = reserveBalance,
                availableBalance = availableBalance,
                hasCurrentMonthReserveContribution = hasActiveMonthlyReserveContribution(
                    inputs.reserveEvents,
                    referenceInstant,
                ),
            )
        }

    private fun calculateMonthlyBudgetSummary(
        plan: FinancialPlan?,
        movements: List<Movement>,
        referenceInstant: java.time.Instant,
    ): MonthlyBudgetSummary? {
        val plannedIncome = plan?.estimatedMonthlyIncome?.takeUnless(Money::isZero) ?: return null
        val monthRange = cycleRangeCalculator.currentCycleRange(BudgetCycle.MONTHLY, referenceInstant)
        val actualExpenses = movements
            .asSequence()
            .filter { movement ->
                movement.type == MovementType.EXPENSE &&
                    movement.status == MovementStatus.CONFIRMED &&
                    movement.recordedAt >= monthRange.start &&
                    movement.recordedAt < monthRange.end
            }
            .fold(Money.ZERO) { total, movement -> total + movement.amount }
        val isOverBudget = actualExpenses.amount > plannedIncome.amount
        val remaining = if (isOverBudget) {
            Money.ZERO
        } else {
            when (val result = plannedIncome - actualExpenses) {
                is pe.kipu.core.domain.model.DomainResult.Ok -> result.value
                is pe.kipu.core.domain.model.DomainResult.Err -> Money.ZERO
            }
        }
        return MonthlyBudgetSummary(
            plannedIncome = plannedIncome,
            actualExpenses = actualExpenses,
            remaining = remaining,
            isOverBudget = isOverBudget,
        )
    }

    private companion object {
        const val RECENT_MOVEMENTS_LIMIT = 3
        const val DEFAULT_ALERT_PERCENT = 80
    }

    /** Agrupa las 5 entradas reactivas para combinarlas luego con el tick de tiempo. */
    private data class HomeInputs(
        val budgets: List<pe.kipu.core.domain.model.EnvelopeBudgetState>,
        val movements: List<pe.kipu.core.domain.model.Movement>,
        val commitments: List<pe.kipu.core.domain.model.Commitment>,
        val categories: List<Category>,
        val preferences: pe.kipu.core.domain.model.UserPreferences? = null,
        val plan: FinancialPlan?,
        val reserveEvents: List<pe.kipu.core.domain.model.ReserveEvent> = emptyList(),
    )
}
