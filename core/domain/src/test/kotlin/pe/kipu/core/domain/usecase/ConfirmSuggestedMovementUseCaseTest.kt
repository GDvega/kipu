package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.DuplicateConfirmationRequiredException
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository

class ConfirmSuggestedMovementUseCaseTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")
    private val repository = RecordingMovementRepository()
    private val useCase = ConfirmSuggestedMovementUseCase(
        confirmWithDuplicateCheck = ConfirmSuggestedMovementWithDuplicateCheckUseCase(
            movementRepository = repository,
            detectDuplicateMovement = DetectDuplicateMovementUseCase(MovementDuplicateMatcher()),
        ),
    )

    @Test
    fun `does not save when suggestion validation fails`() = runTest {
        val invalid = SuggestedMovement(
            draftId = "draft-invalid",
            source = MovementSource.MANUAL,
            amount = Money.of(BigDecimal("5.00")).getOrError(),
        )

        val result = useCase(
            suggestion = invalid,
            movementId = "movement-1",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
        )

        assertTrue(result.isFailure)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `saves confirmed movement when data is complete`() = runTest {
        val suggestion = SuggestedMovement(
            draftId = "draft-1",
            source = MovementSource.RECEIPT,
            confidence = SuggestionConfidence.HIGH,
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("25.50")).getOrError(),
            channel = PaymentChannel.YAPE,
            counterpartyName = "MARIA",
            operationReference = "OP-1",
        )

        val result = useCase(
            suggestion = suggestion,
            movementId = "movement-1",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, repository.saveCount)
        val saved = repository.lastSaved
        assertEquals(MovementStatus.CONFIRMED, saved?.status)
        assertEquals("OP-1", saved?.operationNumber)
    }

    @Test
    fun `uses suggested recorded at when explicit recorded at is omitted`() = runTest {
        val peruZone = ZoneId.of("America/Lima")
        val suggestedAt = ZonedDateTime.of(2026, 6, 16, 15, 45, 0, 0, peruZone).toInstant()
        val suggestion = SuggestedMovement(
            draftId = "draft-3",
            source = MovementSource.RECEIPT,
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("25.50")).getOrError(),
            channel = PaymentChannel.YAPE,
            counterpartyName = "MARIA",
            suggestedRecordedAt = suggestedAt,
        )

        val result = useCase(
            suggestion = suggestion,
            movementId = "movement-3",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
        )

        assertTrue(result.isSuccess)
        assertEquals(suggestedAt, repository.lastSaved?.recordedAt)
    }

    @Test
    fun `parser suggestion alone does not persist`() = runTest {
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `does not save when duplicate exists and no resolution`() = runTest {
        repository.movements = listOf(existingMovement())

        val result = useCase(
            suggestion = completeSuggestion(),
            movementId = "movement-new",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DuplicateConfirmationRequiredException)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `saves when duplicate exists and user chooses save as new`() = runTest {
        repository.movements = listOf(existingMovement())

        val result = useCase(
            suggestion = completeSuggestion(),
            movementId = "movement-new",
            categoryId = CategoryIds.FOOD,
            createdAt = now,
            recordedAt = now,
            resolution = DuplicateResolution.SAVE_AS_NEW,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `rejects zero amount confirmed movement`() = runTest {
        val suggestion = SuggestedMovement(
            draftId = "draft-2",
            source = MovementSource.RECEIPT,
            type = MovementType.EXPENSE,
            amount = Money.ZERO,
            channel = PaymentChannel.PLIN,
            counterpartyName = "ANA",
        )

        val result = useCase(
            suggestion = suggestion,
            movementId = "movement-2",
            categoryId = CategoryIds.OTHER,
            createdAt = now,
            recordedAt = now,
        )

        assertFalse(result.isSuccess)
        assertEquals(0, repository.saveCount)
    }

    private fun completeSuggestion(): SuggestedMovement = SuggestedMovement(
        draftId = "draft-1",
        source = MovementSource.RECEIPT,
        confidence = SuggestionConfidence.HIGH,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        channel = PaymentChannel.YAPE,
        counterpartyName = "MARIA",
        operationReference = "OP-1",
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
        var lastSaved: Movement? = null

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = null

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saveCount += 1
            lastSaved = movement
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
