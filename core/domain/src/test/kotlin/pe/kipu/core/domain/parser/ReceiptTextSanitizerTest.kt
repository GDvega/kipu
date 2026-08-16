package pe.kipu.core.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptTextSanitizerTest {

    @Test
    fun sanitize_trimsLeadingAndTrailingWhitespace() {
        val input = "   Yape Pagaste S/ 10.00   "
        assertEquals("Yape Pagaste S/ 10.00", ReceiptTextSanitizer.sanitize(input))
    }

    @Test
    fun sanitize_collapsesMultipleSpacesNewlinesAndTabs() {
        val input = "Yape\n\n\tPagaste   S/\r\n10.00\t\ta  JUAN"
        assertEquals("Yape Pagaste S/ 10.00 a JUAN", ReceiptTextSanitizer.sanitize(input))
    }

    @Test
    fun sanitize_truncatesAtMaxLength() {
        val longString = "A".repeat(ReceiptTextSanitizer.MAX_LENGTH + 500)
        val result = ReceiptTextSanitizer.sanitize(longString)

        assertEquals(ReceiptTextSanitizer.MAX_LENGTH, result.length)
        assertEquals("A".repeat(ReceiptTextSanitizer.MAX_LENGTH), result)
    }

    @Test
    fun sanitize_emptyAndBlankStrings_returnEmpty() {
        assertEquals("", ReceiptTextSanitizer.sanitize(""))
        assertEquals("", ReceiptTextSanitizer.sanitize("   \n\t  "))
    }
}
