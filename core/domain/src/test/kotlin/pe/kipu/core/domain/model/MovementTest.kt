package pe.kipu.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class MovementTest {

    private val validAmount = Money.of(BigDecimal("15.00")).getOrError()
    private val now = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun validate_acceptsConfirmedMovement() {
        val movement = sampleMovement(status = MovementStatus.CONFIRMED)
        assertTrue(movement.validate() is DomainResult.Ok)
    }

    @Test
    fun validate_rejectsBlankId() {
        val movement = sampleMovement(id = "")
        assertTrue(movement.validate() is DomainResult.Err)
    }

    @Test
    fun validate_rejectsZeroAmountWhenConfirmed() {
        val movement = sampleMovement(
            amount = Money.ZERO,
            status = MovementStatus.CONFIRMED,
        )
        val result = movement.validate()
        assertTrue(result is DomainResult.Err)
        assertEquals(
            "Confirmed movement amount must be greater than zero",
            (result as DomainResult.Err).error.message,
        )
    }

    @Test
    fun validate_rejectsManualPendingConfirmation() {
        val movement = sampleMovement(
            source = MovementSource.MANUAL,
            status = MovementStatus.PENDING_CONFIRMATION,
        )
        assertTrue(movement.validate() is DomainResult.Err)
    }

    @Test
    fun validate_rejectsBlankOperationNumber() {
        val movement = sampleMovement(operationNumber = "")
        assertTrue(movement.validate() is DomainResult.Err)
    }

    @Test
    fun validate_acceptsOperationNumber() {
        val movement = sampleMovement(operationNumber = "OP-999")
        assertTrue(movement.validate() is DomainResult.Ok)
    }

    private fun sampleMovement(
        id: String = "movement-1",
        amount: Money = validAmount,
        status: MovementStatus = MovementStatus.CONFIRMED,
        source: MovementSource = MovementSource.MANUAL,
        operationNumber: String? = null,
    ) = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = amount,
        categoryId = "category-1",
        channel = PaymentChannel.YAPE,
        source = source,
        status = status,
        operationNumber = operationNumber,
        recordedAt = now,
        createdAt = now,
    )
}
