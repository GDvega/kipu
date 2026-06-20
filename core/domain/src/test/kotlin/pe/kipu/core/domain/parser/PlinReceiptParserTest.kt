package pe.kipu.core.domain.parser

import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.test.ReceiptFixtureLoader

class PlinReceiptParserTest {

    private val parser = PlinReceiptParser()

    @Test
    fun `extracts amount and counterparty`() {
        val text = ReceiptFixtureLoader.load("receipts/plin_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(BigDecimal("10.50"), result.suggestion.amount?.amount)
        assertEquals("LUIS LOPEZ", result.suggestion.counterpartyName)
    }

    @Test
    fun `extracts suggested recorded at from standard fixture`() {
        val text = ReceiptFixtureLoader.load("receipts/plin_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        val peruZone = ZoneId.of("America/Lima")
        val expected = ZonedDateTime.of(2026, 6, 16, 14, 15, 0, 0, peruZone).toInstant()
        assertEquals(expected, result.suggestion.suggestedRecordedAt)
        assertEquals(SuggestionConfidence.HIGH, result.suggestion.confidence)
    }

    @Test
    fun `receipt without message has null message`() {
        val text = ReceiptFixtureLoader.load("receipts/plin_no_message.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertNull(result.suggestion.message)
    }

    @Test
    fun `rejects text that does not look like plin`() {
        val text = ReceiptFixtureLoader.load("receipts/unknown_channel.txt")
        val result = parser.parse(text)

        assertTrue(result is ReceiptParseResult.Failure)
    }

    @Test
    fun `incomplete receipt has low confidence`() {
        val text = ReceiptFixtureLoader.load("receipts/plin_incomplete.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(SuggestionConfidence.LOW, result.suggestion.confidence)
    }

    @Test
    fun `sets plin channel`() {
        val text = ReceiptFixtureLoader.load("receipts/plin_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(PaymentChannel.PLIN, result.suggestion.channel)
    }
}
