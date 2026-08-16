package pe.kipu.core.domain.parser

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.test.ReceiptFixtureLoader

class ReceiptDateTimeParserTest {

    private val peruZone: ZoneId = ZoneId.of("America/Lima")

    @Test
    fun `parses spanish date and time with pm marker`() {
        val sanitized = ReceiptTextSanitizer.sanitize(
            ReceiptFixtureLoader.load("receipts/yape_standard.txt"),
        )

        val instant = ReceiptDateTimeParser.parse(sanitized)

        val expected = ZonedDateTime.of(2026, 6, 16, 15, 45, 0, 0, peruZone).toInstant()
        assertEquals(expected, instant)
    }

    @Test
    fun `parses plin spanish date and time`() {
        val sanitized = ReceiptTextSanitizer.sanitize(
            ReceiptFixtureLoader.load("receipts/plin_standard.txt"),
        )

        val instant = ReceiptDateTimeParser.parse(sanitized)

        val expected = ZonedDateTime.of(2026, 6, 16, 14, 15, 0, 0, peruZone).toInstant()
        assertEquals(expected, instant)
    }

    @Test
    fun `parses slash date with 24 hour time`() {
        val text = "Yape Pagaste S/ 0.10 Para TIENDA 15/06/2026 10:30 Operación 998877"

        val instant = ReceiptDateTimeParser.parse(text)

        val expected = ZonedDateTime.of(2026, 6, 15, 10, 30, 0, 0, peruZone).toInstant()
        assertEquals(expected, instant)
    }

    @Test
    fun `returns null when only date without time`() {
        val text = "Yape S/ 10 Para JUAN 16/06/2026 Nro de operación 123"

        assertNull(ReceiptDateTimeParser.parse(text))
    }

    @Test
    fun `returns null for blank text`() {
        assertNull(ReceiptDateTimeParser.parse(""))
    }

    @Test
    fun `returns null for out of range slash date`() {
        val text = "Yape Pagaste S/ 10.00 Para TIENDA 99/99/9999 10:30"
        assertNull(ReceiptDateTimeParser.parse(text))
    }

    @Test
    fun `returns null for invalid february date`() {
        val text = "Yape Pagaste S/ 10.00 Para TIENDA 31/02/2026 10:30"
        assertNull(ReceiptDateTimeParser.parse(text))
    }

    @Test
    fun `returns null for invalid hour or minute`() {
        val invalidHour = "Yape Pagaste S/ 10.00 Para TIENDA 15/06/2026 25:30"
        assertNull(ReceiptDateTimeParser.parse(invalidHour))

        val invalidMinute = "Yape Pagaste S/ 10.00 Para TIENDA 15/06/2026 10:75"
        assertNull(ReceiptDateTimeParser.parse(invalidMinute))
    }
}
