package pe.kipu.core.data.mapper

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError

class ReserveEventMapperTest {
    @Test
    fun `domain to entity round trip preserves reserve audit data`() {
        val original = ReserveEvent(
            id = "reserve-use-1",
            type = ReserveEventType.USE,
            amount = Money.of(BigDecimal("300.00")).getOrError(),
            sourceMovementId = "movement-1",
            occurredAt = Instant.parse("2026-08-26T15:00:00Z"),
            createdAt = Instant.parse("2026-08-26T15:01:00Z"),
        )

        assertEquals(original, original.toEntity().toDomain())
    }
}
