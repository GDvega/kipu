package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.data.local.entity.GatheringEntity
import pe.kipu.core.domain.model.Gathering

class GatheringMapperTest {

    @Test
    fun roundTripPreservesParticipants() {
        val domain = Gathering(
            id = "gathering-1",
            name = "Asado",
            participantCount = 3,
            participantNames = listOf("Ana", "Luis", "Pedro"),
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()

        assertEquals(domain, restored)
        assertEquals("Ana|Luis|Pedro", entity.participantNames)
    }

    @Test
    fun decodeEmptyParticipantNames() {
        val entity = GatheringEntity(
            id = "gathering-2",
            name = "Solo",
            participantCount = 1,
            participantNames = "",
        )

        assertEquals(emptyList<String>(), entity.toDomain().participantNames)
    }
}
