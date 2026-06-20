package pe.kipu.core.domain.duplicate

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError

class MovementDuplicateMatcherTest {

    private val matcher = MovementDuplicateMatcher()
    private val baseInstant = Instant.parse("2026-06-16T15:00:00Z")

    @Test
    fun `matches same amount counterparty and time within tolerance`() {
        val movementA = movement(
            id = "a",
            amount = "25.00",
            counterparty = "Maria Lopez",
            recordedAt = baseInstant,
        )
        val movementB = movement(
            id = "b",
            amount = "25.00",
            counterparty = "MARIA  LOPEZ",
            recordedAt = baseInstant.plusSeconds(10 * 60),
        )

        assertEquals(
            DuplicateMatchReasonKeys.AMOUNT_COUNTERPARTY_TIME,
            matcher.findMatchReasonKey(movementA, movementB),
        )
        assertTrue(matcher.isDuplicate(movementA, movementB))
    }

    @Test
    fun `does not match when date is outside tolerance`() {
        val movementA = movement(
            id = "a",
            amount = "25.00",
            counterparty = "Maria",
            recordedAt = baseInstant,
        )
        val movementB = movement(
            id = "b",
            amount = "25.00",
            counterparty = "Maria",
            recordedAt = baseInstant.plusSeconds(20 * 60),
        )

        assertNull(matcher.findMatchReasonKey(movementA, movementB))
        assertFalse(matcher.isDuplicate(movementA, movementB))
    }

    @Test
    fun `does not match when amount differs`() {
        val movementA = movement(id = "a", amount = "25.00", counterparty = "Maria", recordedAt = baseInstant)
        val movementB = movement(id = "b", amount = "30.00", counterparty = "Maria", recordedAt = baseInstant)

        assertNull(matcher.findMatchReasonKey(movementA, movementB))
    }

    @Test
    fun `does not match when counterparty differs`() {
        val movementA = movement(id = "a", amount = "25.00", counterparty = "Maria", recordedAt = baseInstant)
        val movementB = movement(id = "b", amount = "25.00", counterparty = "Ana", recordedAt = baseInstant)

        assertNull(matcher.findMatchReasonKey(movementA, movementB))
    }

    @Test
    fun `does not match when both counterparties are blank`() {
        val movementA = movement(id = "a", amount = "25.00", counterparty = null, recordedAt = baseInstant)
        val movementB = movement(id = "b", amount = "25.00", counterparty = "   ", recordedAt = baseInstant)

        assertNull(matcher.findMatchReasonKey(movementA, movementB))
    }

    @Test
    fun `matches by operation number even when date exceeds tolerance`() {
        val movementA = movement(
            id = "a",
            amount = "25.00",
            counterparty = "Ana",
            recordedAt = baseInstant,
            operationNumber = "OP-123",
        )
        val movementB = movement(
            id = "b",
            amount = "25.00",
            counterparty = "Pedro",
            recordedAt = baseInstant.plusSeconds(3_600),
            operationNumber = "op-123",
        )

        assertEquals(
            DuplicateMatchReasonKeys.OPERATION_NUMBER,
            matcher.findMatchReasonKey(movementA, movementB),
        )
    }

    @Test
    fun `does not match by operation number when amount differs`() {
        val movementA = movement(
            id = "a",
            amount = "10.00",
            counterparty = "Ana",
            recordedAt = baseInstant,
            operationNumber = "OP-123",
        )
        val movementB = movement(
            id = "b",
            amount = "99.00",
            counterparty = "Pedro",
            recordedAt = baseInstant.plusSeconds(3_600),
            operationNumber = "op-123",
        )

        assertNull(matcher.findMatchReasonKey(movementA, movementB))
    }

    @Test
    fun `does not match pending confirmation movements`() {
        val confirmed = movement(id = "a", amount = "25.00", counterparty = "Maria", recordedAt = baseInstant)
        val pending = movement(
            id = "b",
            amount = "25.00",
            counterparty = "Maria",
            recordedAt = baseInstant,
            status = MovementStatus.PENDING_CONFIRMATION,
        )

        assertNull(matcher.findMatchReasonKey(confirmed, pending))
    }

    private fun movement(
        id: String,
        amount: String,
        counterparty: String?,
        recordedAt: Instant,
        operationNumber: String? = null,
        status: MovementStatus = MovementStatus.CONFIRMED,
    ): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = status,
        counterpartyName = counterparty,
        operationNumber = operationNumber,
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )
}
