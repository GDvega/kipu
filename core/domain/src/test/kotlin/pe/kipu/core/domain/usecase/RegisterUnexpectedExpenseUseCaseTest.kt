package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.LocalTransactionRunner
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt

class RegisterUnexpectedExpenseUseCaseTest {
    @Test
    fun `registers purchase and reserve use inside one transaction`() = runTest {
        val transactionRunner = CountingTransactionRunner()
        val movements = RecordingMovementRepository()
        val reserves = RecordingReserveRepository()
        val envelopes = RecordingEnvelopeRepository()
        val prepare = preparePreview(movements, reserves, envelopes, transactionRunner)
        val preview = prepare(money("300.00"))
        assertNotNull(preview.snapshot)
        assertEquals(money("100.00"), preview.coverage.fromReserve)
        transactionRunner.invocations = 0
        val create = CreateManualMovementUseCase(
            movements,
            RecordingAuditRepository(),
            FixedTimeProvider,
            reserves,
            transactionRunner,
        )
        val useCase = RegisterUnexpectedExpenseUseCase(
            create,
            ApplyRecoveryPlanUseCase(envelopes, transactionRunner),
            prepare,
            transactionRunner,
        )

        val result = useCase(
            amount = money("300.00"),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            expectedPreview = preview,
            reserveAmount = money("100.00"),
            recoveryPlan = preview.recoveryPlan,
        )

        assertTrue(result.isSuccess)
        assertEquals(money("300.00"), movements.saved.single().amount)
        assertEquals(money("100.00"), reserves.recorded.single().amount)
        assertEquals(ReserveEventType.USE, reserves.recorded.single().type)
        assertTrue(transactionRunner.invocations >= 2)
    }

    private fun preparePreview(
        movements: MovementRepository,
        reserves: ReserveEventRepository,
        envelopes: EnvelopeRepository,
        transactionRunner: LocalTransactionRunner,
    ): PrepareUnexpectedExpenseUseCase {
        val plans = InitialPlanRepository()
        val receipts = EmptyReceiptRepository()
        return PrepareUnexpectedExpenseUseCase(
            movementRepository = movements,
            reserveEventRepository = reserves,
            financialPlanRepository = plans,
            commitmentRepository = EmptyCommitmentRepository(),
            observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
                envelopeRepository = envelopes,
                movementRepository = movements,
                gatheringExpenseRepository = EmptyGatheringExpenseRepository(),
                monthlyServiceReceiptRepository = receipts,
                financialPlanRepository = plans,
                calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(CalculateCategoryPeriodSpentUseCase()),
                cycleRangeCalculator = CycleRangeCalculator(FixedTimeProvider),
                timeProvider = FixedTimeProvider,
            ),
            observeMonthlyServiceReceipts = ObserveMonthlyServiceReceiptsUseCase(
                plans, receipts, movements, FixedTimeProvider,
            ),
            timeProvider = FixedTimeProvider,
            calculateCoverage = CalculateUnexpectedExpenseCoverageUseCase(),
            buildRecoveryPlan = BuildUnexpectedExpenseRecoveryPlanUseCase(),
            localTransactionRunner = transactionRunner,
        )
    }

    private inner class InitialPlanRepository : FinancialPlanRepository {
        private val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = money("2000.00"),
            fixedExpenses = Money.ZERO,
            initialBalance = money("1000.00"),
        )
        override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(listOf(plan))
        override suspend fun getById(id: String): FinancialPlan? = plan.takeIf { it.id == id }
        override suspend fun save(plan: FinancialPlan): Result<Unit> = error("Unexpected plan write")
        override suspend fun delete(id: String): Result<Unit> = error("Unexpected plan deletion")
    }

    private class EmptyCommitmentRepository : CommitmentRepository {
        override fun observeCommitments(): Flow<List<Commitment>> = flowOf(emptyList())
        override suspend fun getById(id: String): Commitment? = null
        override suspend fun save(commitment: Commitment): Result<Unit> = error("Unexpected commitment write")
        override suspend fun delete(id: String): Result<Unit> = error("Unexpected commitment deletion")
    }

    private class EmptyReceiptRepository : MonthlyServiceReceiptRepository {
        override fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>> = flowOf(emptyList())
        override fun observeAllPaidMovementIds(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) = error("Unexpected receipt write")
        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt? = null
    }

    private class EmptyGatheringExpenseRepository : GatheringExpenseRepository {
        override fun observeTotalsByGathering(): Flow<Map<String, Money>> = flowOf(emptyMap())
        override fun observeExpensesByGathering(): Flow<Map<String, List<GatheringExpense>>> = flowOf(emptyMap())
        override fun observeLinkedMovementIds(): Flow<Set<String>> = flowOf(emptySet())
        override fun observeActiveGatheringLinkedMovementIds(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun isMovementLinked(movementId: String): Boolean = false
        override suspend fun save(expense: GatheringExpense): Result<Unit> = error("Unexpected gathering expense write")
    }

    private object FixedTimeProvider : TimeProvider {
        override fun now(): Instant = Instant.parse("2026-08-29T12:00:00Z")
    }

    private class CountingTransactionRunner : LocalTransactionRunner {
        var invocations = 0
        override suspend fun <T> run(block: suspend () -> T): Result<T> = runCatching {
            invocations++
            block()
        }
    }

    private class RecordingMovementRepository : MovementRepository {
        val saved = mutableListOf<Movement>()
        override fun observeMovements(): Flow<List<Movement>> = flowOf(saved)
        override suspend fun getById(id: String): Movement? = saved.find { it.id == id }
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit).also { saved += movement }
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class RecordingAuditRepository : MovementAuditRepository {
        override fun observeAuditLogs(): Flow<List<MovementAuditEntry>> = flowOf(emptyList())
        override suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit> = Result.success(Unit)
        override suspend fun getAll(): List<MovementAuditEntry> = emptyList()
    }

    private inner class RecordingReserveRepository : ReserveEventRepository {
        private val initial = ReserveEvent(
            id = "reserve-contribution-before-purchase",
            type = ReserveEventType.CONTRIBUTION,
            amount = money("100.00"),
            occurredAt = FixedTimeProvider.now(),
            createdAt = FixedTimeProvider.now(),
        )
        val recorded = mutableListOf<ReserveEvent>()
        override fun observeAll(): Flow<List<ReserveEvent>> = flowOf(listOf(initial) + recorded)
        override suspend fun getById(id: String): ReserveEvent? = (listOf(initial) + recorded).find { it.id == id }
        override suspend fun record(event: ReserveEvent): Result<Unit> = Result.success(Unit).also { recorded += event }
    }

    private class RecordingEnvelopeRepository : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(emptyList())
        override suspend fun getById(id: String): Envelope? = null
        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()
}
