package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository

class UpdateMovementUseCaseTest {

    private val now = Instant.parse("2026-08-22T12:00:00Z")
    private val timeProvider = object : pe.kipu.core.domain.time.TimeProvider {
        override fun now(): Instant = now
    }

    @Test
    fun `updates movement successfully and records audit log when valid`() = runTest {
        val original = sampleMovement(id = "mov-1", type = MovementType.EXPENSE, categoryId = CategoryIds.FOOD)
        val movementRepository = FakeMovementRepository(listOf(original))
        val categoryRepository = FakeCategoryRepository(
            listOf(Category(id = CategoryIds.FOOD, name = "Comida"), Category(id = CategoryIds.SERVICES, name = "Servicios")),
        )
        val auditRepository = FakeMovementAuditRepository()
        val useCase = newUseCase(movementRepository, categoryRepository, auditRepository)

        val newAmount = Money.of(BigDecimal("85.50")).getOrError()
        val result = useCase(
            movementId = "mov-1",
            type = MovementType.EXPENSE,
            amount = newAmount,
            categoryId = CategoryIds.SERVICES,
            channel = PaymentChannel.YAPE,
            description = "Pago de luz",
            counterpartyName = "Enel",
        )

        assertTrue(result.isSuccess)
        val saved = movementRepository.saved
        assertEquals(newAmount, saved?.amount)
        assertEquals(CategoryIds.SERVICES, saved?.categoryId)
        assertEquals(PaymentChannel.YAPE, saved?.channel)
        assertEquals("Pago de luz", saved?.description)
        assertEquals("Enel", saved?.counterpartyName)
        assertEquals(original.recordedAt, saved?.recordedAt)

        assertEquals(1, auditRepository.recordedLogs.size)
        assertEquals("mov-1", auditRepository.recordedLogs[0].movementId)
        assertEquals(pe.kipu.core.domain.model.MovementAuditAction.UPDATED, auditRepository.recordedLogs[0].action)
        assertTrue(auditRepository.recordedLogs[0].details!!.contains("Monto:"))
    }

    @Test
    fun `clears commitmentId when converting income with goal to expense`() = runTest {
        val original = sampleMovement(
            id = "mov-income",
            type = MovementType.INCOME,
            categoryId = CategoryIds.OTHER,
            commitmentId = "goal-vacations",
        )
        val movementRepository = FakeMovementRepository(listOf(original))
        val categoryRepository = FakeCategoryRepository(listOf(Category(id = CategoryIds.FOOD, name = "Comida")))
        val auditRepository = FakeMovementAuditRepository()
        val useCase = newUseCase(movementRepository, categoryRepository, auditRepository)

        val result = useCase(
            movementId = "mov-income",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("50.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
            description = null,
            counterpartyName = null,
        )

        assertTrue(result.isSuccess)
        val saved = movementRepository.saved
        assertEquals(MovementType.EXPENSE, saved?.type)
        assertNull(saved?.commitmentId)
    }

    @Test
    fun `fails when movement not found`() = runTest {
        val movementRepository = FakeMovementRepository(emptyList())
        val categoryRepository = FakeCategoryRepository(listOf(Category(id = CategoryIds.FOOD, name = "Comida")))
        val auditRepository = FakeMovementAuditRepository()
        val useCase = newUseCase(movementRepository, categoryRepository, auditRepository)

        val result = useCase(
            movementId = "non-existent",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("10.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
            description = null,
            counterpartyName = null,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when category does not exist`() = runTest {
        val original = sampleMovement(id = "mov-1", type = MovementType.EXPENSE, categoryId = CategoryIds.FOOD)
        val movementRepository = FakeMovementRepository(listOf(original))
        val categoryRepository = FakeCategoryRepository(emptyList())
        val auditRepository = FakeMovementAuditRepository()
        val useCase = newUseCase(movementRepository, categoryRepository, auditRepository)

        val result = useCase(
            movementId = "mov-1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("10.00")).getOrError(),
            categoryId = "unknown-cat",
            channel = PaymentChannel.CASH,
            description = null,
            counterpartyName = null,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when amount is zero for confirmed movement`() = runTest {
        val original = sampleMovement(id = "mov-1", type = MovementType.EXPENSE, categoryId = CategoryIds.FOOD)
        val movementRepository = FakeMovementRepository(listOf(original))
        val categoryRepository = FakeCategoryRepository(listOf(Category(id = CategoryIds.FOOD, name = "Comida")))
        val auditRepository = FakeMovementAuditRepository()
        val useCase = newUseCase(movementRepository, categoryRepository, auditRepository)

        val result = useCase(
            movementId = "mov-1",
            type = MovementType.EXPENSE,
            amount = Money.ZERO,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
            description = null,
            counterpartyName = null,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `recalculates reserve use when covered expense amount decreases`() = runTest {
        val original = sampleMovement("mov-covered", MovementType.EXPENSE, CategoryIds.FOOD)
            .copy(amount = Money.of(BigDecimal("100.00")).getOrError())
        val movementRepository = FakeMovementRepository(listOf(original))
        val categoryRepository = FakeCategoryRepository(listOf(Category(CategoryIds.FOOD, "Comida")))
        val reserveUse = ReserveEvent(
            id = "reserve-use-original",
            type = ReserveEventType.USE,
            amount = original.amount,
            sourceMovementId = original.id,
            occurredAt = now,
            createdAt = now,
        )
        val reserveRepository = FakeReserveRepository(mutableListOf(reserveUse))
        val useCase = UpdateMovementUseCase(
            movementRepository,
            categoryRepository,
            FakeMovementAuditRepository(),
            reserveRepository,
            timeProvider,
        )

        val newAmount = Money.of(BigDecimal("60.00")).getOrError()
        val result = useCase(
            movementId = original.id,
            type = MovementType.EXPENSE,
            amount = newAmount,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.CASH,
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(ReserveEventType.REVERSAL, ReserveEventType.USE), reserveRepository.recorded.map { it.type })
        assertEquals(reserveUse.id, reserveRepository.recorded.first().reversesEventId)
        assertEquals(newAmount, reserveRepository.recorded.last().amount)
    }

    private fun newUseCase(
        movementRepository: MovementRepository,
        categoryRepository: CategoryRepository,
        auditRepository: FakeMovementAuditRepository,
    ) = UpdateMovementUseCase(
        movementRepository,
        categoryRepository,
        auditRepository,
        FakeReserveRepository(),
        timeProvider,
    )

    private class FakeMovementAuditRepository : pe.kipu.core.domain.repository.MovementAuditRepository {
        val recordedLogs = mutableListOf<pe.kipu.core.domain.model.MovementAuditEntry>()
        override fun observeAuditLogs(): Flow<List<pe.kipu.core.domain.model.MovementAuditEntry>> = flowOf(recordedLogs)
        override suspend fun recordAudit(entry: pe.kipu.core.domain.model.MovementAuditEntry): Result<Unit> {
            recordedLogs.add(entry)
            return Result.success(Unit)
        }
        override suspend fun getAll(): List<pe.kipu.core.domain.model.MovementAuditEntry> = recordedLogs
    }

    private fun sampleMovement(
        id: String,
        type: MovementType,
        categoryId: String,
        commitmentId: String? = null,
    ): Movement = Movement(
        id = id,
        type = type,
        amount = Money.of(BigDecimal("10.00")).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        commitmentId = commitmentId,
        recordedAt = now,
        createdAt = now,
    )

    private class FakeMovementRepository(
        private val movements: List<Movement>,
    ) : MovementRepository {
        var saved: Movement? = null

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saved = movement
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(categories)

        override suspend fun getById(id: String): Category? = categories.find { it.id == id }

        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
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
