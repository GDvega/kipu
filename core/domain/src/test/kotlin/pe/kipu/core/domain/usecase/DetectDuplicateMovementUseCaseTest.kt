package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.DuplicateDetectionResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError

class DetectDuplicateMovementUseCaseTest {

    private val useCase = DetectDuplicateMovementUseCase(MovementDuplicateMatcher())
    private val baseInstant = Instant.parse("2026-06-16T15:00:00Z")

    @Test
    fun `detects duplicate with same amount counterparty and nearby time`() {
        val existing = movement("existing", "25.00", "Maria", baseInstant)
        val candidate = movement("candidate", "25.00", "MARIA", baseInstant.plusSeconds(300))

        val result = useCase(candidate, listOf(existing))

        assertTrue(result is DuplicateDetectionResult.Matches)
        assertEquals(listOf(existing), (result as DuplicateDetectionResult.Matches).existing)
    }

    @Test
    fun `returns no match when date is outside tolerance`() {
        val existing = movement("existing", "25.00", "Maria", baseInstant)
        val candidate = movement("candidate", "25.00", "Maria", baseInstant.plusSeconds(20 * 60))

        val result = useCase(candidate, listOf(existing))

        assertEquals(DuplicateDetectionResult.NoMatch, result)
    }

    @Test
    fun `returns no match when amount differs`() {
        val existing = movement("existing", "25.00", "Maria", baseInstant)
        val candidate = movement("candidate", "30.00", "Maria", baseInstant)

        val result = useCase(candidate, listOf(existing))

        assertEquals(DuplicateDetectionResult.NoMatch, result)
    }

    @Test
    fun `returns no match when counterparty differs`() {
        val existing = movement("existing", "25.00", "Maria", baseInstant)
        val candidate = movement("candidate", "25.00", "Ana", baseInstant)

        val result = useCase(candidate, listOf(existing))

        assertEquals(DuplicateDetectionResult.NoMatch, result)
    }

    @Test
    fun `detects duplicate by operation number with same amount`() {
        val existing = movement("existing", "10.00", "A", baseInstant, operationNumber = "OP-9")
        val candidate = movement(
            id = "candidate",
            amount = "10.00",
            counterparty = "B",
            recordedAt = baseInstant.plusSeconds(3_600),
            operationNumber = "OP-9",
        )

        val result = useCase(candidate, listOf(existing))

        assertTrue(result is DuplicateDetectionResult.Matches)
    }

    @Test
    fun `returns no match when operation number matches but amount differs`() {
        val existing = movement("existing", "10.00", "A", baseInstant, operationNumber = "OP-9")
        val candidate = movement("candidate", "99.00", "B", baseInstant, operationNumber = "OP-9")

        val result = useCase(candidate, listOf(existing))

        assertEquals(DuplicateDetectionResult.NoMatch, result)
    }

    @Test
    fun `does not match candidate against itself`() {
        val movement = movement("same", "25.00", "Maria", baseInstant)

        val result = useCase(movement, listOf(movement))

        assertEquals(DuplicateDetectionResult.NoMatch, result)
    }

    private fun movement(
        id: String,
        amount: String,
        counterparty: String,
        recordedAt: Instant,
        operationNumber: String? = null,
    ): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = counterparty,
        operationNumber = operationNumber,
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )
}
