package pe.kipu.core.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class PenAmountParserTest {

    @Test
    fun `parses two decimal places`() {
        val amount = PenAmountParser.parse("Transferiste S/ 10.50 a Juan")
        assertEquals(Money.of(BigDecimal("10.50")).getOrError(), amount)
    }

    @Test
    fun `parses single decimal digit as decimos`() {
        val amount = PenAmountParser.parse("Pagaste S/ 10.5")
        assertEquals(Money.of(BigDecimal("10.50")).getOrError(), amount)
    }

    @Test
    fun `parses amount without fractional part`() {
        val amount = PenAmountParser.parse("S/ 25")
        assertEquals(Money.of(BigDecimal("25.00")).getOrError(), amount)
    }

    @Test
    fun `parses thousands amount without thousands separator`() {
        val amount = PenAmountParser.parse("S/ 1234.50")
        assertEquals(Money.of(BigDecimal("1234.50")).getOrError(), amount)
    }

    @Test
    fun `parses round thousands amount without fractional part`() {
        val amount = PenAmountParser.parse("S/ 3000")
        assertEquals(Money.of(BigDecimal("3000.00")).getOrError(), amount)
    }

    @Test
    fun `parses large amount without thousands separator`() {
        val amount = PenAmountParser.parse("S/ 12345.00")
        assertEquals(Money.of(BigDecimal("12345.00")).getOrError(), amount)
    }

    @Test
    fun `parses amount with thousands separator`() {
        val amount = PenAmountParser.parse("S/ 1,234.50")
        assertEquals(Money.of(BigDecimal("1234.50")).getOrError(), amount)
    }

    @Test
    fun `parses millions with multiple thousands separators`() {
        val amount = PenAmountParser.parse("S/ 1,000,000.00")
        assertEquals(Money.of(BigDecimal("1000000.00")).getOrError(), amount)
    }

    @Test
    fun `returns null when no amount present`() {
        assertNull(PenAmountParser.parse("Hola"))
    }
}
