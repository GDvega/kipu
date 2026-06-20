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
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository

class ResolveDuplicateMovementUseCaseTest {

    private val repository = RecordingMovementRepository()
    private val useCase = ResolveDuplicateMovementUseCase(repository)
    private val olderInstant = Instant.parse("2026-06-16T10:00:00Z")
    private val newerInstant = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `merge deletes the most recent movement in the pair`() = runTest {
        val older = movement("older", olderInstant)
        val newer = movement("newer", newerInstant)
        val pair = MovementDuplicatePair(
            movementA = older,
            movementB = newer,
            matchReasonKey = "duplicate_amount_counterparty_time",
        )

        val result = useCase(pair, DuplicateResolution.MERGE)

        assertTrue(result.isSuccess)
        assertEquals(listOf("newer"), repository.deletedIds)
    }

    @Test
    fun `save as new does not delete`() = runTest {
        val pair = MovementDuplicatePair(
            movementA = movement("older", olderInstant),
            movementB = movement("newer", newerInstant),
            matchReasonKey = "duplicate_amount_counterparty_time",
        )

        val result = useCase(pair, DuplicateResolution.SAVE_AS_NEW)

        assertTrue(result.isSuccess)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `merge deletes movement with higher id when created at is equal`() = runTest {
        val movementA = movement("movement-a", olderInstant)
        val movementB = movement("movement-b", olderInstant)
        val pair = MovementDuplicatePair(
            movementA = movementA,
            movementB = movementB,
            matchReasonKey = "duplicate_amount_counterparty_time",
        )

        val result = useCase(pair, DuplicateResolution.MERGE)

        assertTrue(result.isSuccess)
        assertEquals(listOf("movement-b"), repository.deletedIds)
    }

    @Test
    fun `cancel does not delete or save`() = runTest {
        val pair = MovementDuplicatePair(
            movementA = movement("older", olderInstant),
            movementB = movement("newer", newerInstant),
            matchReasonKey = "duplicate_amount_counterparty_time",
        )

        val result = useCase(pair, DuplicateResolution.CANCEL)

        assertTrue(result.isSuccess)
        assertTrue(repository.deletedIds.isEmpty())
        assertEquals(0, repository.saveCount)
    }

    private fun movement(id: String, createdAt: Instant): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = "Maria",
        recordedAt = createdAt,
        createdAt = createdAt,
    )

    private class RecordingMovementRepository : MovementRepository {
        val deletedIds = mutableListOf<String>()
        var saveCount: Int = 0

        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())

        override suspend fun getById(id: String): Movement? = null

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saveCount += 1
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> {
            deletedIds += id
            return Result.success(Unit)
        }
    }
}
