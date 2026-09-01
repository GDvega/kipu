package pe.kipu.core.domain.model

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReserveEventTest {

    private val now = Instant.parse("2026-08-26T15:00:00Z")
    private val amount = Money.of(BigDecimal("200.00")).getOrError()

    @Test
    fun `valid contribution is accepted`() {
        val event = ReserveEvent(
            id = "reserve-1",
            type = ReserveEventType.CONTRIBUTION,
            amount = amount,
            occurredAt = now,
            createdAt = now,
        )

        assertTrue(event.validate() is DomainResult.Ok)
    }

    @Test
    fun `use requires source movement`() {
        val event = ReserveEvent(
            id = "reserve-use-1",
            type = ReserveEventType.USE,
            amount = amount,
            occurredAt = now,
            createdAt = now,
        )

        val result = event.validate()

        assertTrue(result is DomainResult.Err)
        assertEquals("Reserve use requires a source movement", (result as DomainResult.Err).error.message)
    }

    @Test
    fun `reversal requires referenced event`() {
        val event = ReserveEvent(
            id = "reserve-reversal-1",
            type = ReserveEventType.REVERSAL,
            amount = amount,
            occurredAt = now,
            createdAt = now,
        )

        assertTrue(event.validate() is DomainResult.Err)
    }
}
