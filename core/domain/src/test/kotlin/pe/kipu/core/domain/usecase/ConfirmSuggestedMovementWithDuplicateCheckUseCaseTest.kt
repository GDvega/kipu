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
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository

class ConfirmSuggestedMovementWithDuplicateCheckUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val repository = RecordingMovementRepository()
    private val auditRepository = FakeMovementAuditRepository()
    private val useCase = ConfirmSuggestedMovementWithDuplicateCheckUseCase(
        movementRepository = repository,
        detectDuplicateMovement = DetectDuplicateMovementUseCase(MovementDuplicateMatcher()),
        movementAuditRepository = auditRepository,
    )

    @Test
    fun `returns duplicate pending without saving when duplicate exists and no resolution`() = runTest {
        repository.movements = listOf(existingMovement())

        val result = useCase(
            suggestion = suggestion(),
            movementId = "movement-new",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
        )

        assertTrue(result is ConfirmMovementResult.DuplicatePending)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `saves when user chooses save as new`() = runTest {
        repository.movements = listOf(existingMovement())

        val result = useCase(
            suggestion = suggestion(),
            movementId = "movement-new",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
            resolution = DuplicateResolution.SAVE_AS_NEW,
        )

        assertTrue(result is ConfirmMovementResult.Saved)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `does not save candidate when user chooses merge`() = runTest {
        repository.movements = listOf(existingMovement())

        val result = useCase(
            suggestion = suggestion(),
            movementId = "movement-new",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
            resolution = DuplicateResolution.MERGE,
        )

        assertEquals(ConfirmMovementResult.Cancelled, result)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `saves normally when no duplicate exists`() = runTest {
        repository.movements = emptyList()

        val result = useCase(
            suggestion = suggestion(),
            movementId = "movement-new",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
        )

        assertTrue(result is ConfirmMovementResult.Saved)
        assertEquals(1, repository.saveCount)
    }

    private fun suggestion(): SuggestedMovement = SuggestedMovement(
        draftId = "draft-1",
        source = MovementSource.RECEIPT,
        confidence = SuggestionConfidence.HIGH,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        channel = PaymentChannel.YAPE,
        counterpartyName = "MARIA",
    )

    private fun existingMovement(): Movement = Movement(
        id = "movement-existing",
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = "Maria",
        recordedAt = now,
        createdAt = now,
    )

    private class RecordingMovementRepository : MovementRepository {
        var movements: List<Movement> = emptyList()
        var saveCount: Int = 0

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName.equals(counterpartyName, ignoreCase = true) }

        override suspend fun save(movement: Movement): Result<Unit> {
            saveCount += 1
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
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
}
