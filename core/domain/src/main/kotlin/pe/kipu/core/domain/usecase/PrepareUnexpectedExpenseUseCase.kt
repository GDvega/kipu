package pe.kipu.core.domain.usecase

import javax.inject.Inject
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.*
import pe.kipu.core.domain.repository.*
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.TimeProvider

class PrepareUnexpectedExpenseUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val reserveEventRepository: ReserveEventRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val commitmentRepository: CommitmentRepository,
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val observeMonthlyServiceReceipts: ObserveMonthlyServiceReceiptsUseCase,
    private val timeProvider: TimeProvider,
    private val calculateCoverage: CalculateUnexpectedExpenseCoverageUseCase,
    private val buildRecoveryPlan: BuildUnexpectedExpenseRecoveryPlanUseCase,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    suspend operator fun invoke(expense: Money): UnexpectedExpensePreview = localTransactionRunner.run {
        require(!expense.isZero()) { "Expense must be positive" }
        val today = timeProvider.now().atZone(CycleRangeCalculator.PERU_ZONE).toLocalDate()
        val snapshot = UnexpectedExpenseSnapshot(
            date = today,
            expense = expense,
            plan = financialPlanRepository.observePlans().first().firstOrNull(),
            movements = movementRepository.observeMovements().first().sortedBy { it.id },
            reserveEvents = reserveEventRepository.observeAll().first().sortedBy { it.id },
            commitments = commitmentRepository.observeCommitments().first().sortedBy { it.id },
            budgets = currentBudgets().sortedBy { it.envelopeId },
            receipts = observeMonthlyServiceReceipts().first().sortedBy { it.key.identifier },
        )
        val cash = CalculateCashFlowSummaryUseCase()(
            snapshot.movements, snapshot.commitments, snapshot.plan?.initialBalance ?: Money.ZERO,
        ).netCash
        val reserve = CalculateReserveBalanceUseCase()(snapshot.reserveEvents).balance
        val obligations = CalculateRemainingPlannedExpensesUseCase()(
            snapshot.plan, snapshot.budgets, snapshot.receipts, today,
        )
        val linkedIncome = snapshot.movements.filter {
            it.status == MovementStatus.CONFIRMED && it.type == MovementType.INCOME && it.commitmentId != null
        }.groupBy { requireNotNull(it.commitmentId) }.mapValues { (_, movements) ->
            movements.fold(Money.ZERO) { total, movement -> total + movement.amount }
        }
        // Same commitment accounting as plan validation; no guessed association with fixed receipts.
        val commitmentBurden = snapshot.plan?.let {
            ValidateFinancialPlanUseCase().analyze(it, emptyList(), snapshot.commitments, linkedIncome).commitmentsBurden
        } ?: Money.of(snapshot.commitments.filter {
            !it.isSettled && it.type != CommitmentType.SAVINGS_GOAL && it.currencyCode == Money.CURRENCY_CODE
        }.sumOf { it.currentAmount?.amount ?: BigDecimal.ZERO }).getOrError()
        val coverage = calculateCoverage(
            expense = expense,
            reserveBalance = reserve,
            availableBalance = cash - reserve.max(BigDecimal.ZERO),
            protectedObligations = obligations + commitmentBurden,
        )
        val adjustableGap = (coverage.uncovered.amount + coverage.existingShortfall.amount - coverage.liquidityGap.amount)
            .max(BigDecimal.ZERO)
        val recovery = buildRecoveryPlan(Money.of(adjustableGap).getOrError(), snapshot.budgets)
        val remaining = recovery.remainingGap + coverage.liquidityGap
        UnexpectedExpensePreview(
            coverage = coverage,
            recoveryPlan = recovery.copy(remainingGap = remaining, isFullyRecoverable = remaining.isZero()),
            snapshot = snapshot,
        )
    }.getOrThrow()

    suspend fun currentBudgets(): List<EnvelopeBudgetState> = observeEnvelopeBudgets().first()
}
