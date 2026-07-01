package pe.kipu.core.domain.plan

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError

class SavingsGoalBurdenCalculatorTest {

    @Test
    fun `monthly burden spreads remaining target across horizon months`() {
        val target = Money.of(BigDecimal("1000.00")).getOrError()
        val current = Money.of(BigDecimal("150.00")).getOrError()

        val monthly = SavingsGoalBurdenCalculator.monthlyBurden(
            target = target,
            current = current,
            horizonMonths = 5,
            currencyCode = "PEN",
        )

        assertEquals(Money.of(BigDecimal("170.00")).getOrError(), monthly)
    }

    @Test
    fun `monthly burden uses default horizon when months missing`() {
        val target = Money.of(BigDecimal("500.00")).getOrError()
        val current = Money.of(BigDecimal("120.00")).getOrError()

        val monthly = SavingsGoalBurdenCalculator.monthlyBurden(
            target = target,
            current = current,
            horizonMonths = null,
            currencyCode = "PEN",
        )

        assertEquals(Money.of(BigDecimal("76.00")).getOrError(), monthly)
    }

    @Test
    fun `weekly contribution matches monthly burden divided by four`() {
        val remaining = Money.of(BigDecimal("850.00")).getOrError()

        val weekly = SavingsGoalBurdenCalculator.weeklyContribution(remaining, horizonMonths = 5).getOrError()
        val monthly = SavingsGoalBurdenCalculator.monthlyBurden(
            target = Money.of(BigDecimal("1000.00")).getOrError(),
            current = Money.of(BigDecimal("150.00")).getOrError(),
            horizonMonths = 5,
            currencyCode = "PEN",
        )

        assertEquals(Money.of(BigDecimal("42.50")).getOrError(), weekly)
        assertEquals(Money.of(BigDecimal("170.00")).getOrError(), monthly)
    }
}
