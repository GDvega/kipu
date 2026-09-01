package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError

class CalculateUnexpectedExpenseCoverageUseCaseTest {
    private val useCase = CalculateUnexpectedExpenseCoverageUseCase()

    @Test
    fun `purchase uses reserve first then available balance and exposes the gap`() {
        val result = useCase(
            expense = money("300.00"),
            reserveBalance = BigDecimal("100.00"),
            availableBalance = BigDecimal("100.00"),
        )

        assertEquals(BigDecimal("100.00"), result.fromReserve.amount)
        assertEquals(BigDecimal("100.00"), result.fromAvailableBalance.amount)
        assertEquals(BigDecimal("100.00"), result.uncovered.amount)
        assertFalse(result.isFullyCovered)
    }

    @Test
    fun `reserve alone can cover the purchase`() {
        val result = useCase(
            expense = money("50.00"),
            reserveBalance = BigDecimal("100.00"),
            availableBalance = BigDecimal("20.00"),
        )

        assertEquals(BigDecimal("50.00"), result.fromReserve.amount)
        assertEquals(BigDecimal("0.00"), result.fromAvailableBalance.amount)
        assertEquals(BigDecimal("0.00"), result.uncovered.amount)
        assertTrue(result.isFullyCovered)
    }

    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()
}
