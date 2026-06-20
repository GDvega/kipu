package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.data.local.entity.EnvelopeEntity
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.data.local.seed.DefaultEnvelopeIds
import pe.kipu.core.domain.model.Money
import java.math.BigDecimal

class EnvelopeMapperTest {

    @Test
    fun `entity to domain to entity round trip`() {
        val original = EnvelopeEntity(
            id = DefaultEnvelopeIds.FOOD,
            name = "Comida",
            weeklyLimitCents = 15_000L,
            categoryId = CategoryIds.FOOD,
        )

        val domain = original.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals(original, roundTrip)
        assertEquals(Money.ZERO, domain.spentAmount)
    }

    @Test
    fun `maps weekly limit cents to money`() {
        val entity = EnvelopeEntity(
            id = DefaultEnvelopeIds.TRANSPORT,
            name = "Transporte",
            weeklyLimitCents = 8_050L,
            categoryId = CategoryIds.TRANSPORT,
        )

        val domain = entity.toDomain()

        assertEquals(BigDecimal("80.50"), domain.weeklyLimit.amount)
    }
}
