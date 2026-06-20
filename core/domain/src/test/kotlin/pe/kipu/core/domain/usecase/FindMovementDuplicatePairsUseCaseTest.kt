package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError

class FindMovementDuplicatePairsUseCaseTest {

    private val useCase = FindMovementDuplicatePairsUseCase(MovementDuplicateMatcher())
    private val baseInstant = Instant.parse("2026-06-16T15:00:00Z")

    @Test
    fun `returns one pair when two movements are duplicates`() {
        val movementA = movement("movement-a", "25.00", "Maria", baseInstant)
        val movementB = movement("movement-b", "25.00", "Maria", baseInstant.plusSeconds(120))

        val pairs = useCase(listOf(movementA, movementB))

        assertEquals(1, pairs.size)
        assertEquals("movement-a", pairs.first().movementA.id)
        assertEquals("movement-b", pairs.first().movementB.id)
    }

    @Test
    fun `returns empty list when there are no duplicates`() {
        val movementA = movement("movement-a", "25.00", "Maria", baseInstant)
        val movementB = movement("movement-b", "30.00", "Ana", baseInstant)

        val pairs = useCase(listOf(movementA, movementB))

        assertTrue(pairs.isEmpty())
    }

    @Test
    fun `does not alert on a single movement`() {
        val movement = movement("movement-a", "25.00", "Maria", baseInstant)

        val pairs = useCase(listOf(movement))

        assertTrue(pairs.isEmpty())
    }

    private fun movement(
        id: String,
        amount: String,
        counterparty: String,
        recordedAt: Instant,
    ): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = counterparty,
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )
}
