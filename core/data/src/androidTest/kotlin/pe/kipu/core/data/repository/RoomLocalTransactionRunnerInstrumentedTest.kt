package pe.kipu.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.runBlocking
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
    fun reserveFailureRollsBackUnexpectedExpenseAndAudit() = runBlocking {
        val now = Instant.parse("2026-08-23T10:00:00Z")
        val movementId = "manual-${now.toEpochMilli()}"
        val reserveRepository = RoomReserveEventRepository(database.reserveEventDao())
        reserveRepository.record(
            ReserveEvent(
                id = "existing-use",
                type = ReserveEventType.USE,
                amount = Money.of(BigDecimal("5.00")).getOrError(),
                sourceMovementId = movementId,
                occurredAt = now,
                createdAt = now,
            ),
        ).getOrThrow()
        val useCase = CreateManualMovementUseCase(
            movementRepository = RoomMovementRepository(database.movementDao()),
            movementAuditRepository = RoomMovementAuditRepository(database.movementAuditDao()),
            timeProvider = { now },
            reserveEventRepository = reserveRepository,
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
        assertNull(database.movementDao().getById(movementId))
        assertTrue(database.movementAuditDao().getAll().isEmpty())
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
        val movementRepository = RoomMovementRepository(database.movementDao())
        val auditRepository = RoomMovementAuditRepository(database.movementAuditDao())
        val reserveRepository = RoomReserveEventRepository(database.reserveEventDao())
        val envelopeRepository = RoomEnvelopeRepository(database.envelopeDao())
        val register = RegisterUnexpectedExpenseUseCase(
            createManualMovement = CreateManualMovementUseCase(
                movementRepository,
                auditRepository,
                { now },
                reserveRepository,
                runner,
            ),
            applyRecoveryPlan = ApplyRecoveryPlanUseCase(envelopeRepository, runner),
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
            reserveAmount = money("100.00"),
            recoveryPlan = staleProposal,
        )

        assertTrue(result.isFailure)
        assertNull(database.movementDao().getById("manual-${now.toEpochMilli()}"))
        assertTrue(database.movementAuditDao().getAll().isEmpty())
        assertTrue(reserveRepository.observeAll().first().isEmpty())
        assertEquals(currentEnvelope.weeklyLimit, envelopeRepository.getById(currentEnvelope.id)?.weeklyLimit)
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()
}
