package pe.kipu.core.domain.util

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult

class MoneyInputParserTest {

    @Test
    fun `parses comma as decimal separator when cents are provided`() {
        assertAmount("1,50", "1.50")
    }

    @Test
    fun `parses grouped spanish amount with decimal comma`() {
        assertAmount("1.500,50", "1500.50")
    }

    @Test
    fun `parses grouped international amount with decimal point`() {
        assertAmount("1,500.50", "1500.50")
    }

    @Test
    fun `keeps a single three digit group as thousands`() {
        assertAmount("1,500", "1500.00")
        assertAmount("1.500", "1500.00")
    }

    @Test
    fun `rejects malformed separator groups`() {
        val result = MoneyInputParser.parsePen("1,2,3")

        assertTrue(result is DomainResult.Err)
    }

    private fun assertAmount(input: String, expected: String) {
        val result = MoneyInputParser.parsePen(input)

        assertTrue(result is DomainResult.Ok)
        result as DomainResult.Ok
        assertEquals(0, result.value.amount.compareTo(BigDecimal(expected)))
    }
}
