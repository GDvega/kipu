package pe.kipu.core.designsystem.component

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class KipuAmountTextTest {

    @Test
    fun formatsPositiveAmountWithoutSign() {
        val result = formatPenAmountForDisplay(BigDecimal("1500.00"), showSign = false)
        assertEquals("S/ 1,500.00", result)
    }

    @Test
    fun formatsNegativeAmountWithoutExplicitShowSign() {
        val result = formatPenAmountForDisplay(BigDecimal("-30.00"), showSign = false)
        assertEquals("- S/ 30.00", result)
    }

    @Test
    fun formatsPositiveAmountWithShowSign() {
        val result = formatPenAmountForDisplay(BigDecimal("40.00"), showSign = true)
        assertEquals("+ S/ 40.00", result)
    }

    @Test
    fun formatsNegativeAmountWithShowSign() {
        val result = formatPenAmountForDisplay(BigDecimal("-40.00"), showSign = true)
        assertEquals("- S/ 40.00", result)
    }

    @Test
    fun formatsZero() {
        val result = formatPenAmountForDisplay(BigDecimal.ZERO)
        assertEquals("S/ 0.00", result)
    }
}
