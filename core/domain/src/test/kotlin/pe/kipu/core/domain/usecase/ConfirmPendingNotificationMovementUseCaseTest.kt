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
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository

class ConfirmPendingNotificationMovementUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val repository = RecordingMovementRepository()
    private val useCase = ConfirmPendingNotificationMovementUseCase(
        movementRepository = repository,
        detectDuplicateMovement = DetectDuplicateMovementUseCase(MovementDuplicateMatcher()),
    )

    @Test
    fun `promotes pending notification to confirmed when no duplicate`() = runTest {
        repository.movements = listOf(pendingMovement())

        val result = useCase("movement-pending-1")

        assertTrue(result is ConfirmMovementResult.Saved)
        val saved = repository.movements.first { it.id == "movement-pending-1" }
        assertEquals(MovementStatus.CONFIRMED, saved.status)
    }

    @Test
    fun `blocks promotion when duplicate confirmed exists without resolution`() = runTest {
        repository.movements = listOf(pendingMovement(), confirmedDuplicate())

        val result = useCase("movement-pending-1")

        assertTrue(result is ConfirmMovementResult.DuplicatePending)
        assertEquals(MovementStatus.PENDING_CONFIRMATION, repository.movements.first { it.id == "movement-pending-1" }.status)
    }

    @Test
    fun `promotes when user chooses save as new`() = runTest {
        repository.movements = listOf(pendingMovement(), confirmedDuplicate())

        val result = useCase("movement-pending-1", DuplicateResolution.SAVE_AS_NEW)

        assertTrue(result is ConfirmMovementResult.Saved)
        assertEquals(MovementStatus.CONFIRMED, repository.movements.first { it.id == "movement-pending-1" }.status)
    }

    private fun pendingMovement(): Movement = Movement(
        id = "movement-pending-1",
        type = MovementType.INCOME,
        amount = Money.of(BigDecimal("50.00")).getOrError(),
        categoryId = CategoryIds.OTHER,
        channel = PaymentChannel.YAPE,
        source = MovementSource.NOTIFICATION,
        status = MovementStatus.PENDING_CONFIRMATION,
        counterpartyName = "MARIA GARCIA RIOS",
        recordedAt = now,
        createdAt = now,
    )

    private fun confirmedDuplicate(): Movement = pendingMovement().copy(
        id = "movement-confirmed-1",
        status = MovementStatus.CONFIRMED,
    )

    private class RecordingMovementRepository : MovementRepository {
        var movements: List<Movement> = emptyList()
        var saveCount: Int = 0

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saveCount++
            movements = movements.filter { it.id != movement.id } + movement
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
