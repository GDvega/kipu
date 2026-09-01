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
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository

class DeleteMovementUseCaseTest {

    private val now = Instant.parse("2026-08-22T12:00:00Z")
    private val timeProvider = object : pe.kipu.core.domain.time.TimeProvider {
        override fun now(): Instant = now
    }

    @Test
    fun `deletes movement successfully and records audit log when it exists`() = runTest {
        val movement = sampleMovement("mov-1")
        val repository = FakeMovementRepository(mutableListOf(movement))
        val auditRepository = FakeMovementAuditRepository()
        val useCase = DeleteMovementUseCase(repository, auditRepository, FakeReserveRepository(), timeProvider)

        val result = useCase("mov-1")

        assertTrue(result.isSuccess)
        assertEquals("mov-1", repository.deletedId)
        assertEquals(1, auditRepository.recordedLogs.size)
        assertEquals("mov-1", auditRepository.recordedLogs[0].movementId)
        assertEquals(pe.kipu.core.domain.model.MovementAuditAction.DELETED, auditRepository.recordedLogs[0].action)
    }

    @Test
    fun `fails when movement id is blank`() = runTest {
        val repository = FakeMovementRepository(mutableListOf())
        val auditRepository = FakeMovementAuditRepository()
        val useCase = DeleteMovementUseCase(repository, auditRepository, FakeReserveRepository(), timeProvider)

        val result = useCase("   ")

        assertTrue(result.isFailure)
    }

    @Test
    fun `propagates repository error when deletion fails`() = runTest {
        val repository = FailingMovementRepository()
        val auditRepository = FakeMovementAuditRepository()
        val useCase = DeleteMovementUseCase(repository, auditRepository, FakeReserveRepository(), timeProvider)

        val result = useCase("mov-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `reverses active reserve use when deleting covered movement`() = runTest {
        val movement = sampleMovement("mov-covered")
        val repository = FakeMovementRepository(mutableListOf(movement))
        val reserveUse = ReserveEvent(
            id = "reserve-use-mov-covered",
            type = ReserveEventType.USE,
            amount = Money.of(BigDecimal("20.00")).getOrError(),
            sourceMovementId = movement.id,
            occurredAt = now,
            createdAt = now,
        )
        val reserveRepository = FakeReserveRepository(mutableListOf(reserveUse))
        val useCase = DeleteMovementUseCase(
            repository,
            FakeMovementAuditRepository(),
            reserveRepository,
            timeProvider,
        )

        val result = useCase(movement.id)

        assertTrue(result.isSuccess)
        val reversal = reserveRepository.recorded.single()
        assertEquals(ReserveEventType.REVERSAL, reversal.type)
        assertEquals(reserveUse.id, reversal.reversesEventId)
        assertEquals(reserveUse.amount, reversal.amount)
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

    private fun sampleMovement(id: String): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.CASH,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        recordedAt = now,
        createdAt = now,
    )

    private class FakeMovementRepository(
        private val movements: MutableList<Movement>,
    ) : MovementRepository {
        var deletedId: String? = null

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> {
            deletedId = id
            movements.removeAll { it.id == id }
            return Result.success(Unit)
        }
    }

    private class FailingMovementRepository : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())

        override suspend fun getById(id: String): Movement? = null

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> =
            Result.failure(IllegalStateException("Database failure"))
    }

    private class FakeReserveRepository(
        private val events: MutableList<ReserveEvent> = mutableListOf(),
    ) : ReserveEventRepository {
        val recorded = mutableListOf<ReserveEvent>()
        override fun observeAll(): Flow<List<ReserveEvent>> = flowOf(events)
        override suspend fun getById(id: String): ReserveEvent? = events.find { it.id == id }
        override suspend fun record(event: ReserveEvent): Result<Unit> {
            recorded += event
            events += event
            return Result.success(Unit)
        }
    }
}
