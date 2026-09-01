package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError

class CalculateReserveBalanceUseCaseTest {
    private val useCase = CalculateReserveBalanceUseCase()
    private val now = Instant.parse("2026-08-26T15:00:00Z")

    @Test
    fun `unused monthly contributions accumulate`() {
        val result = useCase(
            listOf(
                event("july", ReserveEventType.CONTRIBUTION, "100.00"),
                event("august", ReserveEventType.CONTRIBUTION, "100.00"),
            ),
        )

        assertEquals(BigDecimal("200.00"), result.balance)
        assertFalse(result.isOverdrawn)
    }

    @Test
    fun `use refund and reversal are reflected without rewriting history`() {
        val result = useCase(
            listOf(
                event("contribution", ReserveEventType.CONTRIBUTION, "200.00"),
                event("use", ReserveEventType.USE, "100.00", sourceMovementId = "purchase"),
                event("refund", ReserveEventType.REFUND, "25.00", sourceMovementId = "refund-movement"),
                event(
                    id = "reverse-refund",
                    type = ReserveEventType.REVERSAL,
                    amount = "25.00",
                    reversesEventId = "refund",
                ),
            ),
        )

        assertEquals(BigDecimal("100.00"), result.balance)
        assertEquals(BigDecimal("200.00"), result.totalAdded.amount)
        assertEquals(BigDecimal("100.00"), result.totalUsed.amount)
    }

    private fun event(
        id: String,
        type: ReserveEventType,
        amount: String,
        sourceMovementId: String? = null,
        reversesEventId: String? = null,
    ) = ReserveEvent(
        id = id,
        type = type,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        sourceMovementId = sourceMovementId,
        reversesEventId = reversesEventId,
        occurredAt = now,
        createdAt = now,
    )
}
