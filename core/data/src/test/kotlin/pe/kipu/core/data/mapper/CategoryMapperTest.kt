package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.data.test.MapperTestFixtures

class CategoryMapperTest {

    @Test
    fun `entity to domain to entity round trip`() {
        val original = MapperTestFixtures.sampleCategoryEntity()

        val domain = original.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals(original, roundTrip)
    }

    @Test
    fun `maps nullable icon key`() {
        val entity = MapperTestFixtures.sampleCategoryEntity().copy(iconKey = null)

        val domain = entity.toDomain()

        assertEquals(null, domain.iconKey)
        assertEquals("Comida", domain.name)
    }
}
