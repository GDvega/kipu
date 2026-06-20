package pe.kipu.core.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class CurrencyConverterTest {

    @Test
    fun toPen_convertsUsdUsingReferenceRate() {
        val usd = Money.of(BigDecimal("100.00")).getOrError()

        val pen = CurrencyConverter.toPen(usd, GoalCurrency.USD.code)

        assertEquals("375.00", pen.amount.toPlainString())
    }

    @Test
    fun toPen_keepsPenUnchanged() {
        val pen = Money.of(BigDecimal("250.00")).getOrError()

        val result = CurrencyConverter.toPen(pen, GoalCurrency.PEN.code)

        assertEquals("250.00", result.amount.toPlainString())
    }
}
