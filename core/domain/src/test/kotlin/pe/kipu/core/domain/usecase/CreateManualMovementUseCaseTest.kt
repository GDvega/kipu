package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.time.TimeProvider

class CreateManualMovementUseCaseTest {

    private val fixedInstant = Instant.parse("2026-06-16T15:30:00Z")
    private val repository = RecordingMovementRepository()
    private val auditRepository = FakeMovementAuditRepository()
    private val reserveRepository = RecordingReserveEventRepository()
    private val useCase = CreateManualMovementUseCase(
        movementRepository = repository,
        movementAuditRepository = auditRepository,
        timeProvider = FixedTimeProvider(fixedInstant),
        reserveEventRepository = reserveRepository,
    )

    @Test
    fun savesConfirmedCashExpense() = runTest {
        val amount = Money.of(BigDecimal("25.50")).getOrError()

        val result = useCase(
            type = MovementType.EXPENSE,
            amount = amount,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
            description = "Mercado de barrio",
            commitmentId = "goal-1",
        )

        assertTrue(result.isSuccess)
        val saved = repository.lastSaved
        requireNotNull(saved)
        assertEquals(MovementType.EXPENSE, saved.type)
        assertEquals(PaymentChannel.CASH, saved.channel)
        assertEquals(MovementSource.MANUAL, saved.source)
        assertEquals(MovementStatus.CONFIRMED, saved.status)
        assertEquals("manual-${fixedInstant.toEpochMilli()}", saved.id)
        assertEquals("Mercado de barrio", saved.description)
        assertEquals("goal-1", saved.commitmentId)
    }

    @Test
    fun rejectsZeroAmount() = runTest {
        val result = useCase(
            type = MovementType.EXPENSE,
            amount = Money.ZERO,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
        )

        assertTrue(result.isFailure)
        assertEquals(0, repository.savedCount)
    }

    @Test
    fun savesEnvelopeAssignmentAndReserveUseForUnexpectedExpense() = runTest {
        val result = useCase(
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("300.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            envelopeId = "envelope-leisure",
            reserveAmount = Money.of(BigDecimal("100.00")).getOrError(),
        )

        assertTrue(result.isSuccess)
        assertEquals("envelope-leisure", repository.lastSaved?.envelopeId)
        assertEquals(BigDecimal("100.00"), reserveRepository.lastRecorded?.amount?.amount)
        assertEquals(repository.lastSaved?.id, reserveRepository.lastRecorded?.sourceMovementId)
    }

    @Test
    fun rejectsReserveUseGreaterThanExpenseBeforeWriting() = runTest {
        val savedBefore = repository.savedCount

        val result = useCase(
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            reserveAmount = Money.of(BigDecimal("101.00")).getOrError(),
        )

        assertTrue(result.isFailure)
        assertEquals(savedBefore, repository.savedCount)
    }

    private class FixedTimeProvider(private val instant: Instant) : TimeProvider {
        override fun now(): Instant = instant
    }

    private class RecordingMovementRepository : MovementRepository {
        var lastSaved: Movement? = null
        var savedCount: Int = 0

        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())

        override suspend fun getById(id: EntityId): Movement? = null

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            lastSaved = movement
            savedCount++
            return Result.success(Unit)
        }

        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeMovementAuditRepository : pe.kipu.core.domain.repository.MovementAuditRepository {
        val recordedLogs = mutableListOf<pe.kipu.core.domain.model.MovementAuditEntry>()
        override fun observeAuditLogs(): Flow<List<pe.kipu.core.domain.model.MovementAuditEntry>> = flowOf(recordedLogs)
        override suspend fun recordAudit(entry: pe.kipu.core.domain.model.MovementAuditEntry): Result<Unit> {
            recordedLogs.add(entry)
            return Result.success(Unit)
        }
        override suspend fun getAll(): List<pe.kipu.core.domain.model.MovementAuditEntry> = recordedLogs
    }

    private class RecordingReserveEventRepository : ReserveEventRepository {
        var lastRecorded: ReserveEvent? = null

        override fun observeAll(): Flow<List<ReserveEvent>> = flowOf(emptyList())

        override suspend fun getById(id: String): ReserveEvent? = null

        override suspend fun record(event: ReserveEvent): Result<Unit> {
            lastRecorded = event
            return Result.success(Unit)
        }
    }
}
