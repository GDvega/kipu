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
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider

class CreateManualMovementUseCaseTest {

    private val fixedInstant = Instant.parse("2026-06-16T15:30:00Z")
    private val repository = RecordingMovementRepository()
    private val useCase = CreateManualMovementUseCase(
        movementRepository = repository,
        timeProvider = FixedTimeProvider(fixedInstant),
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
}
