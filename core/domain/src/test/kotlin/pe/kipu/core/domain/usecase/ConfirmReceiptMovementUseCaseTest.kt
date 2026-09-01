package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.FixedTimeProvider

class ConfirmReceiptMovementUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val repository = FakeMovementRepository()
    private val auditRepository = FakeMovementAuditRepository()
    private val useCase = ConfirmReceiptMovementUseCase(
        confirmWithDuplicateCheck = ConfirmSuggestedMovementWithDuplicateCheckUseCase(
            movementRepository = repository,
            detectDuplicateMovement = DetectDuplicateMovementUseCase(MovementDuplicateMatcher()),
            movementAuditRepository = auditRepository,
        ),
        timeProvider = FixedTimeProvider(now),
    )

    @Test
    fun savesReceiptWithDraftBasedMovementId() = runTest {
        val result = useCase(
            suggestion = receiptSuggestion(),
            categoryId = "category-food",
            recordedAt = now,
        )

        assertTrue(result is ConfirmMovementResult.Saved)
        assertEquals("movement-draft-op-ABC123", repository.saved.last().id)
    }

    @Test
    fun mergeResolutionDoesNotPersistDuplicate() = runTest {
        val existing = Movement(
            id = "movement-existing",
            type = MovementType.EXPENSE,
            amount = (Money.of(BigDecimal("25.00")) as DomainResult.Ok).value,
            categoryId = "category-food",
            channel = PaymentChannel.YAPE,
            source = MovementSource.RECEIPT,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "Maria",
            operationNumber = "ABC123",
            recordedAt = now,
            createdAt = now,
        )
        repository.setExisting(listOf(existing))

        val result = useCase(
            suggestion = receiptSuggestion(),
            categoryId = "category-food",
            recordedAt = now,
            resolution = DuplicateResolution.MERGE,
        )

        assertTrue(result is ConfirmMovementResult.Cancelled)
        assertEquals(0, repository.saved.size)
    }

    private fun receiptSuggestion() = SuggestedMovement(
        draftId = "draft-op-ABC123",
        source = MovementSource.RECEIPT,
        type = MovementType.EXPENSE,
        amount = (Money.of(BigDecimal("25.00")) as DomainResult.Ok).value,
        channel = PaymentChannel.YAPE,
        counterpartyName = "Maria",
        operationReference = "ABC123",
        suggestedRecordedAt = now,
    )

    private class FakeMovementRepository : MovementRepository {
        val saved = mutableListOf<Movement>()
        private val movements = MutableStateFlow<List<Movement>>(emptyList())

        override fun observeMovements() = movements

        fun setExisting(existing: List<Movement>) {
            movements.value = existing
        }

        override suspend fun getById(id: String): Movement? = movements.value.find { it.id == id }

        override suspend fun save(movement: Movement): Result<Unit> {
            saved += movement
            movements.value = movements.value.filter { it.id != movement.id } + movement
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> {
            movements.value = movements.value.filter { it.id != id }
            return Result.success(Unit)
        }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.value.filter { it.counterpartyName == counterpartyName }
    }

    private class FakeMovementAuditRepository : pe.kipu.core.domain.repository.MovementAuditRepository {
        val recordedLogs = mutableListOf<pe.kipu.core.domain.model.MovementAuditEntry>()
        override fun observeAuditLogs(): kotlinx.coroutines.flow.Flow<List<pe.kipu.core.domain.model.MovementAuditEntry>> =
            MutableStateFlow(recordedLogs)
        override suspend fun recordAudit(entry: pe.kipu.core.domain.model.MovementAuditEntry): Result<Unit> {
            recordedLogs.add(entry)
            return Result.success(Unit)
        }
        override suspend fun getAll(): List<pe.kipu.core.domain.model.MovementAuditEntry> = recordedLogs
    }
}
