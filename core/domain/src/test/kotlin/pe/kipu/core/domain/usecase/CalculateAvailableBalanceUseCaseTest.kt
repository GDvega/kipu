package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.CashFlowSummary
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveBalance

class CalculateAvailableBalanceUseCaseTest {
    private val useCase = CalculateAvailableBalanceUseCase()

    @Test
    fun `available balance keeps accumulated cash that was not reserved or spent`() {
        val result = useCase(cashFlow(netCash = "1500.00"), reserve(balance = "300.00"))

        assertEquals(BigDecimal("1200.00"), result.availableBalance)
    }

    @Test
    fun `negative cash remains visible instead of being clamped to zero`() {
        val result = useCase(cashFlow(netCash = "-50.00"), reserve(balance = "0.00"))

        assertEquals(BigDecimal("-50.00"), result.availableBalance)
        assertTrue(result.isOverdrawn)
    }

    private fun cashFlow(netCash: String) = CashFlowSummary(
        totalIncome = Money.ZERO,
        totalExpenses = Money.ZERO,
        netCash = BigDecimal(netCash),
        totalGoalRemaining = Money.ZERO,
        isGoalAtRisk = false,
    )

    private fun reserve(balance: String) = ReserveBalance(
        totalAdded = Money.ZERO,
        totalUsed = Money.ZERO,
        balance = BigDecimal(balance),
        isOverdrawn = BigDecimal(balance).signum() < 0,
    )
}
