package pe.kipu.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult

class GatheringParticipantParserTest {

    @Test
    fun parsesLinesAndCommas() {
        val result = GatheringParticipantParser.parse("Ana\nLuis, Pedro")

        assertTrue(result is DomainResult.Ok)
        assertEquals(listOf("Ana", "Luis", "Pedro"), (result as DomainResult.Ok).value)
    }

    @Test
    fun rejectsEmptyInput() {
        val result = GatheringParticipantParser.parse("  \n  ")

        assertTrue(result is DomainResult.Err)
    }
}
