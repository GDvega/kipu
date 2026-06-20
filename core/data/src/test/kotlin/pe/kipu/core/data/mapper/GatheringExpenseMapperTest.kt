package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal
import java.time.Instant

class GatheringExpenseMapperTest {

    @Test
    fun roundTripPreservesPaidByAndMovementId() {
        val domain = GatheringExpense(
            id = "expense-1",
            gatheringId = "gathering-1",
            amount = Money.of(BigDecimal("45.50")).getOrError(),
            paidByParticipant = "Ana",
            description = "Cena",
            movementId = "movement-1",
            recordedAt = Instant.parse("2026-06-16T20:00:00Z"),
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertEquals(domain, restored)
        assertEquals("Ana", entity.paidByParticipant)
        assertEquals("movement-1", entity.movementId)
    }
}
