package pe.kipu.core.data.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class MovementMapperTest {

    @Test
    fun `entity to domain to entity round trip`() {
        val original = MapperTestFixtures.sampleMovementEntity()

        val domain = original.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals(original, roundTrip)
    }

    @Test
    fun `maps enums and instants correctly`() {
        val entity = MapperTestFixtures.sampleMovementEntity()

        val domain = entity.toDomain()

        assertEquals(MovementType.EXPENSE, domain.type)
        assertEquals(PaymentChannel.YAPE, domain.channel)
        assertEquals(MovementSource.MANUAL, domain.source)
        assertEquals(MovementStatus.CONFIRMED, domain.status)
        assertEquals(MapperTestFixtures.recordedAt, domain.recordedAt)
        assertEquals(MapperTestFixtures.createdAt, domain.createdAt)
    }

    @Test
    fun `maps money cents to domain amount`() {
        val entity = MapperTestFixtures.sampleMovementEntity()

        val domain = entity.toDomain()

        assertEquals(BigDecimal("15.50"), domain.amount.amount)
    }

    @Test
    fun `maps domain money to cents`() {
        val amount = Money.of(BigDecimal("99.99")).getOrError()
        val domain = MapperTestFixtures.sampleMovementEntity().toDomain().copy(amount = amount)

        val entity = domain.toEntity()

        assertEquals(9_999L, entity.amountCents)
    }

    @Test
    fun `maps nullable operation number and counterparty`() {
        val entity = MapperTestFixtures.sampleMovementEntity(
            operationNumber = null,
            counterpartyName = null,
            description = "Solo descripción",
        )

        val domain = entity.toDomain()

        assertNull(domain.operationNumber)
        assertNull(domain.counterpartyName)
        assertEquals("Solo descripción", domain.description)
    }

    @Test
    fun `unknown enum values fall back safely`() {
        val entity = MapperTestFixtures.sampleMovementEntity().copy(type = "UNKNOWN_TYPE")

        val domain = entity.toDomain()

        assertEquals(MovementType.EXPENSE, domain.type)
    }

    @Test
    fun `category id is preserved`() {
        val entity = MapperTestFixtures.sampleMovementEntity()

        val domain = entity.toDomain()

        assertEquals(CategoryIds.FOOD, domain.categoryId)
    }
}
