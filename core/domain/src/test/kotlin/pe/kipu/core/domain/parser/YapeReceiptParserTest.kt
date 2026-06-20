package pe.kipu.core.domain.parser

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.test.ReceiptFixtureLoader

class YapeReceiptParserTest {

    private val parser = YapeReceiptParser()

    @Test
    fun `extracts amount S slash 25 dot 50`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(BigDecimal("25.50"), result.suggestion.amount?.amount)
    }

    @Test
    fun `extracts small amount S slash 0 dot 10`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_small_amount.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(BigDecimal("0.10"), result.suggestion.amount?.amount)
    }

    @Test
    fun `extracts amount with thousands separator`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_thousands.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(BigDecimal("1234.50"), result.suggestion.amount?.amount)
    }

    @Test
    fun `extracts counterparty`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals("MARIA GARCIA RIOS", result.suggestion.counterpartyName)
    }

    @Test
    fun `extracts optional message`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals("almuerzo con amigos", result.suggestion.message)
    }

    @Test
    fun `extracts operation number`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals("000123456", result.suggestion.operationReference)
    }

    @Test
    fun `extracts suggested recorded at from standard fixture`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        val peruZone = ZoneId.of("America/Lima")
        val expected = ZonedDateTime.of(2026, 6, 16, 15, 45, 0, 0, peruZone).toInstant()
        assertEquals(expected, result.suggestion.suggestedRecordedAt)
    }

    @Test
    fun `high confidence when date and time present without operation`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_date_time_no_operation.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertNull(result.suggestion.operationReference)
        assertEquals(SuggestionConfidence.HIGH, result.suggestion.confidence)
    }

    @Test
    fun `suggested recorded at is null when only date is present`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_thousands.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertNull(result.suggestion.suggestedRecordedAt)
    }

    @Test
    fun `high confidence when amount counterparty and operation present`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(SuggestionConfidence.HIGH, result.suggestion.confidence)
    }

    @Test
    fun `rejects empty text`() {
        val result = parser.parse("   ")
        assertTrue(result is ReceiptParseResult.Failure)
    }

    @Test
    fun `rejects text without yape signal`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_no_yape_signal.txt")
        val result = parser.parse(text)

        assertTrue(result is ReceiptParseResult.Failure)
        assertEquals(
            "Text does not match Yape receipt",
            (result as ReceiptParseResult.Failure).error.message,
        )
    }

    @Test
    fun `sets yape channel and receipt source`() {
        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = parser.parse(text) as ReceiptParseResult.Success

        assertEquals(PaymentChannel.YAPE, result.suggestion.channel)
    }
}
