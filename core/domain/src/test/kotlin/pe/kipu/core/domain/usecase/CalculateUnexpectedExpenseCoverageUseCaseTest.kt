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

    @Test
    fun `reserve above cash cannot cover more than the actual cash balance`() {
        val reserve = BigDecimal("500.00")
        val available = BigDecimal("-400.00")
        // CalculateAvailableBalanceUseCase: available = netCash - max(reserve, 0).
        val netCash = available + reserve.max(BigDecimal.ZERO)
        assertEquals(BigDecimal("100.00"), netCash)

        val expense = money("300.00")
        val result = useCase(expense, reserve, available)

        assertEquals(BigDecimal("100.00"), result.fromReserve.amount)
        assertEquals(BigDecimal("0.00"), result.fromAvailableBalance.amount)
        assertEquals(BigDecimal("200.00"), result.uncovered.amount)
        assertFalse(result.isFullyCovered)
        assertEquals(
            expense.amount,
            result.fromReserve.amount + result.fromAvailableBalance.amount + result.uncovered.amount,
        )
        assertTrue(result.fromReserve.amount + result.fromAvailableBalance.amount <= netCash)
    }

    @Test
    fun `nominal reserve cannot cover a purchase when actual cash is zero or negative`() {
        val reserve = BigDecimal("500.00")
        for (available in listOf(BigDecimal("-500.00"), BigDecimal("-550.00"))) {
            val netCash = available + reserve.max(BigDecimal.ZERO)
            assertTrue(netCash <= BigDecimal.ZERO)

            val result = useCase(money("50.00"), reserve, available)

            assertEquals(BigDecimal("0.00"), result.fromReserve.amount)
            assertEquals(BigDecimal("0.00"), result.fromAvailableBalance.amount)
            assertEquals(BigDecimal("50.00"), result.uncovered.amount)
            assertFalse(result.isFullyCovered)
        }
    }

    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()

    @Test
    fun `cash earmarked for remaining obligations is not free for a purchase`() {
        val result = useCase(money("300"), BigDecimal("100"), BigDecimal("900"), money("800"))
        assertEquals(money("100"), result.fromReserve)
        assertEquals(money("100"), result.fromAvailableBalance)
        assertEquals(money("100"), result.uncovered)
        assertFalse(result.isFullyCovered)
    }
}
