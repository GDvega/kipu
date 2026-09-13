package pe.kipu.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.seed.DefaultCategorySeed
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.RecoveryEnvelopeAdjustment
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.usecase.CreateManualMovementUseCase
import pe.kipu.core.domain.usecase.ApplyRecoveryPlanUseCase
import pe.kipu.core.domain.usecase.RegisterUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.PrepareUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.ObserveEnvelopeBudgetsUseCase
import pe.kipu.core.domain.usecase.ObserveMonthlyServiceReceiptsUseCase
import pe.kipu.core.domain.usecase.CalculateEnvelopeBudgetStateUseCase
import pe.kipu.core.domain.usecase.CalculateCategoryPeriodSpentUseCase
import pe.kipu.core.domain.usecase.CalculateUnexpectedExpenseCoverageUseCase
import pe.kipu.core.domain.usecase.BuildUnexpectedExpenseRecoveryPlanUseCase
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.TimeProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class RoomLocalTransactionRunnerInstrumentedTest {
    private lateinit var database: KipuDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking { database.categoryDao().insertAll(DefaultCategorySeed.categories) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun failureRollsBackAllRoomWrites() = runBlocking {
        val now = Instant.parse("2026-08-23T10:00:00Z")
        val movement = Movement(
            id = "movement-rollback",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("20.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = now,
            createdAt = now,
        )
        val runner = RoomLocalTransactionRunner(database)

        val result = runner.run {
            database.movementDao().upsert(movement.toEntity())
            error("force rollback")
        }

        assertTrue(result.isFailure)
        assertNull(database.movementDao().getById(movement.id))
    }

    @Test
    fun concurrentGoalContributionsPreserveBothDeltasWithoutCashMovements() = runBlocking {
        val repository = RoomCommitmentRepository(database.commitmentDao())
        val money = Money.of(BigDecimal("50")).getOrError()
        val goal = pe.kipu.core.domain.model.Commitment(
            id = "goal-concurrent", type = pe.kipu.core.domain.model.CommitmentType.SAVINGS_GOAL,
            title = "Laptop", targetAmount = Money.of(BigDecimal("1000")).getOrError(),
            currentAmount = Money.ZERO,
        )
        repository.save(goal).getOrThrow()
        val adjust = pe.kipu.core.domain.usecase.AdjustSavingsGoalContributionUseCase(
            repository, RoomLocalTransactionRunner(database),
        )
        kotlinx.coroutines.coroutineScope {
            val first = async(kotlinx.coroutines.Dispatchers.Default) {
                adjust(goal.id, money, true).getOrThrow()
            }
            val second = async(kotlinx.coroutines.Dispatchers.Default) {
                adjust(goal.id, money, true).getOrThrow()
            }
            first.await()
            second.await()
        }
        assertEquals(Money.of(BigDecimal("100")).getOrError(), repository.getById(goal.id)?.currentAmount)
        assertTrue(database.movementDao().observeAll().first().isEmpty())
    }

    @Test
    fun reserveFailureRollsBackUnexpectedExpenseAndAudit() = runBlocking {
        val now = Instant.parse("2026-08-23T10:00:00Z")
        val reserveRepository = RoomReserveEventRepository(database.reserveEventDao())
        val failingReserveRepository = object : pe.kipu.core.domain.repository.ReserveEventRepository by reserveRepository {
            override suspend fun record(event: ReserveEvent): Result<Unit> {
                reserveRepository.record(event).getOrThrow()
                // Force the actual Room uniqueness failure after all three kinds of writes.
                return reserveRepository.record(event)
            }
        }
        val useCase = CreateManualMovementUseCase(
            movementRepository = RoomMovementRepository(database),
            movementAuditRepository = RoomMovementAuditRepository(database.movementAuditDao()),
            timeProvider = { now },
            reserveEventRepository = failingReserveRepository,
            localTransactionRunner = RoomLocalTransactionRunner(database),
        )

        val result = useCase(
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("20.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            reserveAmount = Money.of(BigDecimal("5.00")).getOrError(),
        )

        assertTrue(result.isFailure)
        assertTrue(database.movementDao().observeAll().first().isEmpty())
        assertTrue(database.movementAuditDao().getAll().isEmpty())
        assertTrue(reserveRepository.observeAll().first().isEmpty())
    }

    @Test
    fun staleRecoveryPlanRollsBackPurchaseReserveAndAudit() = runBlocking {
        val now = Instant.parse("2026-08-29T10:00:00Z")
        val currentEnvelope = Envelope(
            id = "envelope-leisure",
            name = "Ocio",
            weeklyLimit = money("100.00"),
            categoryId = CategoryIds.OTHER,
        )
        database.envelopeDao().upsert(currentEnvelope.toEntity())
        val runner = RoomLocalTransactionRunner(database)
        val movementRepository = RoomMovementRepository(database)
        val auditRepository = RoomMovementAuditRepository(database.movementAuditDao())
        val reserveRepository = RoomReserveEventRepository(database.reserveEventDao())
        val envelopeRepository = RoomEnvelopeRepository(database.envelopeDao())
        val prepare = prepareUnexpectedExpense({ now })
        val register = RegisterUnexpectedExpenseUseCase(
            createManualMovement = CreateManualMovementUseCase(
                movementRepository,
                auditRepository,
                { now },
                reserveRepository,
                runner,
            ),
            applyRecoveryPlan = ApplyRecoveryPlanUseCase(envelopeRepository, runner),
            prepareUnexpectedExpense = prepare,
            localTransactionRunner = runner,
        )
        val staleProposal = UnexpectedExpenseRecoveryPlan(
            adjustments = listOf(
                RecoveryEnvelopeAdjustment(
                    envelopeId = currentEnvelope.id,
                    envelopeName = currentEnvelope.name,
                    currentLimit = money("90.00"),
                    spentAmount = Money.ZERO,
                    proposedLimit = money("50.00"),
                    reduction = money("40.00"),
                ),
            ),
            remainingGap = Money.ZERO,
            isFullyRecoverable = true,
        )

        val result = register(
            amount = money("300.00"),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            envelopeId = currentEnvelope.id,
            reserveAmount = Money.ZERO,
            recoveryPlan = staleProposal,
            expectedPreview = prepare(money("300")),
        )

        assertTrue(result.isFailure)
        assertTrue(database.movementDao().observeAll().first().isEmpty())
        assertTrue(database.movementAuditDao().getAll().isEmpty())
        assertTrue(reserveRepository.observeAll().first().isEmpty())
        assertEquals(currentEnvelope.weeklyLimit, envelopeRepository.getById(currentEnvelope.id)?.weeklyLimit)
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()

    @Test
    fun sameMillisecondManualWritesKeepBothRowsAndTheirAudits() = runBlocking {
        val now = Instant.parse("2026-09-12T12:00:00Z")
        val movements = RoomMovementRepository(database)
        val audit = RoomMovementAuditRepository(database.movementAuditDao())
        val create = CreateManualMovementUseCase(
            movements, audit, { now }, RoomReserveEventRepository(database.reserveEventDao()),
            RoomLocalTransactionRunner(database),
        )
        create(MovementType.EXPENSE, money("25.50"), CategoryIds.FOOD, PaymentChannel.CASH).getOrThrow()
        create(MovementType.EXPENSE, money("80"), CategoryIds.OTHER, PaymentChannel.CASH).getOrThrow()
        val saved = movements.observeMovements().first()
        assertEquals(2, saved.size)
        assertEquals(setOf(money("25.50"), money("80")), saved.map { it.amount }.toSet())
        assertEquals(2, saved.map { it.id }.distinct().size)
        assertEquals(saved.map { it.id }.toSet(), audit.getAll().map { it.movementId }.toSet())
    }

    @Test
    fun purchaseCannotApplyARecoveryLimitBelowItsOwnActualSpending() = runBlocking {
        val now = Instant.parse("2026-09-10T12:00:00Z")
        val plans = RoomFinancialPlanRepository(database.financialPlanDao())
        plans.save(pe.kipu.core.domain.model.FinancialPlan(
            id = "plan", estimatedMonthlyIncome = money("1000"), initialBalance = money("1000"),
            fixedExpenses = money("950"), budgetCycle = pe.kipu.core.domain.model.BudgetCycle.MONTHLY,
        )).getOrThrow()
        val envelopes = RoomEnvelopeRepository(database.envelopeDao())
        val envelope = Envelope(
            id = pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds.LEISURE, name = "Ocio",
            weeklyLimit = money("100"), categoryId = CategoryIds.OTHER,
        )
        envelopes.save(envelope).getOrThrow()
        val movements = RoomMovementRepository(database)
        val previous = Movement(
            id = "previous", type = MovementType.EXPENSE, amount = money("20"), categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH, source = MovementSource.MANUAL, status = MovementStatus.CONFIRMED,
            recordedAt = now, createdAt = now, envelopeId = envelope.id,
        )
        movements.save(previous).getOrThrow()
        val prepare = prepareUnexpectedExpense({ now })
        val preview = prepare(money("70"))
        assertTrue(preview.recoveryPlan.adjustments.isNotEmpty())
        val audit = RoomMovementAuditRepository(database.movementAuditDao())
        val reserves = RoomReserveEventRepository(database.reserveEventDao())
        val runner = RoomLocalTransactionRunner(database)
        val register = RegisterUnexpectedExpenseUseCase(
            CreateManualMovementUseCase(movements, audit, { now }, reserves, runner),
            ApplyRecoveryPlanUseCase(envelopes, runner), prepare, runner,
        )
        val result = register(
            amount = money("70"), categoryId = CategoryIds.OTHER, channel = PaymentChannel.CASH,
            envelopeId = envelope.id, expectedPreview = preview,
            reserveAmount = preview.coverage.fromReserve, recoveryPlan = preview.recoveryPlan,
        )
        assertTrue(result.isFailure)
        assertEquals(listOf(previous), movements.observeMovements().first())
        assertTrue(audit.getAll().isEmpty())
        assertTrue(reserves.observeAll().first().isEmpty())
        assertEquals(envelope, envelopes.getById(envelope.id))
    }

    private fun prepareUnexpectedExpense(time: TimeProvider): PrepareUnexpectedExpenseUseCase {
        val movements = RoomMovementRepository(database)
        val plans = RoomFinancialPlanRepository(database.financialPlanDao())
        val receipts = RoomMonthlyServiceReceiptRepository(database)
        val budgets = ObserveEnvelopeBudgetsUseCase(
            RoomEnvelopeRepository(database.envelopeDao()), movements,
            RoomGatheringExpenseRepository(database.gatheringExpenseDao()), receipts, plans,
            CalculateEnvelopeBudgetStateUseCase(CalculateCategoryPeriodSpentUseCase()),
            CycleRangeCalculator(time), time,
        )
        return PrepareUnexpectedExpenseUseCase(
            movements, RoomReserveEventRepository(database.reserveEventDao()), plans,
            RoomCommitmentRepository(database.commitmentDao()), budgets,
            ObserveMonthlyServiceReceiptsUseCase(plans, receipts, movements, time), time,
            CalculateUnexpectedExpenseCoverageUseCase(), BuildUnexpectedExpenseRecoveryPlanUseCase(),
            RoomLocalTransactionRunner(database),
        )
    }
}
