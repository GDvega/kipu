package pe.kipu.core.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test

class GatheringTest {

    @Test
    fun validateAcceptsConsistentParticipants() {
        val gathering = Gathering(
            id = "gathering-1",
            name = "Asado",
            participantCount = 2,
            participantNames = listOf("Ana", "Luis"),
        )

        assertTrue(gathering.validate() is DomainResult.Ok)
    }

    @Test
    fun validateRejectsMismatchedParticipantCount() {
        val gathering = Gathering(
            id = "gathering-1",
            name = "Asado",
            participantCount = 3,
            participantNames = listOf("Ana", "Luis"),
        )

        assertTrue(gathering.validate() is DomainResult.Err)
    }
}
