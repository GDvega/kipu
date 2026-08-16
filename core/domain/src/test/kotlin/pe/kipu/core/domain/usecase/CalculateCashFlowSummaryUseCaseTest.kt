package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import java.time.Instant

class CalculateCashFlowSummaryUseCaseTest {

    private val useCase = CalculateCashFlowSummaryUseCase()

    @Test
    fun `returns zero when no movements or commitments`() {
        val result = useCase(emptyList(), emptyList())
        
        assertEquals(Money.ZERO, result.totalIncome)
        assertEquals(Money.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("0.00"), result.netCash)
        assertEquals(Money.ZERO, result.totalGoalRemaining)
        assertFalse(result.isGoalAtRisk)
    }

    @Test
    fun `calculates net cash correctly with income and expenses`() {
        val movements = listOf(
            movement("inc1", "100.00", MovementType.INCOME),
            movement("exp1", "30.00", MovementType.EXPENSE),
            movement("exp2", "20.00", MovementType.EXPENSE),
            movement("inc_pend", "500.00", MovementType.INCOME, MovementStatus.PENDING_CONFIRMATION), // Should be ignored
        )
        
        val result = useCase(movements, emptyList())
        
        assertEquals(BigDecimal("100.00"), result.totalIncome.amount)
        assertEquals(BigDecimal("50.00"), result.totalExpenses.amount)
        assertEquals(BigDecimal("50.00"), result.netCash)
        assertFalse(result.isGoalAtRisk)
    }

    @Test
    fun `adds initial balance to real cash`() {
        val movements = listOf(
            movement("inc1", "100.00", MovementType.INCOME),
            movement("exp1", "40.00", MovementType.EXPENSE),
        )

        val result = useCase(
            movements = movements,
            commitments = emptyList(),
            initialBalance = Money.of(BigDecimal("500.00")).getOrError(),
        )

        assertEquals(BigDecimal("560.00"), result.netCash)
    }

    @Test
    fun `keeps negative net cash when expenses exceed income`() {
        val movements = listOf(
            movement("inc1", "50.00", MovementType.INCOME),
            movement("exp1", "100.00", MovementType.EXPENSE),
        )
        
        val result = useCase(movements, emptyList())
        
        assertEquals(BigDecimal("50.00"), result.totalIncome.amount)
        assertEquals(BigDecimal("100.00"), result.totalExpenses.amount)
        assertEquals(BigDecimal("-50.00"), result.netCash)
        assertFalse(result.isGoalAtRisk)
    }

    @Test
    fun `flags goals at risk when net cash is less than targets`() {
        val movements = listOf(
            movement("inc1", "100.00", MovementType.INCOME),
            movement("exp1", "40.00", MovementType.EXPENSE),
        ) // Net cash = 60
        
        val commitments = listOf(
            commitment("goal1", "50.00", isSettled = false), // Target = 50
            commitment("goal2", "20.00", isSettled = false), // Target = 20, Total Target = 70
        )
        
        val result = useCase(movements, commitments)
        
        assertEquals(BigDecimal("60.00"), result.netCash)
        assertEquals(BigDecimal("70.00"), result.totalGoalRemaining.amount)
        assertTrue(result.isGoalAtRisk) // 60 < 70
    }

    @Test
    fun `does not flag goals at risk when net cash is sufficient`() {
        val movements = listOf(
            movement("inc1", "100.00", MovementType.INCOME),
        ) // Net cash = 100
        
        val commitments = listOf(
            commitment("goal1", "50.00", isSettled = false),
            commitment("goal2", "20.00", isSettled = true), // Ignored because settled
        )
        
        val result = useCase(movements, commitments)
        
        assertEquals(BigDecimal("100.00"), result.netCash)
        assertEquals(BigDecimal("50.00"), result.totalGoalRemaining.amount)
        assertFalse(result.isGoalAtRisk) // 100 is not < 50
    }

    @Test
    fun `goal risk compares cash with amount still missing instead of original target`() {
        val movements = listOf(movement("inc1", "500.00", MovementType.INCOME))
        val commitments = listOf(
            commitment(
                id = "goal1",
                targetAmount = "10000.00",
                currentAmount = "9500.00",
                isSettled = false,
            ),
        )

        val result = useCase(movements, commitments)

        assertFalse(result.isGoalAtRisk)
    }

    private fun movement(
        id: String,
        amount: String,
        type: MovementType,
        status: MovementStatus = MovementStatus.CONFIRMED,
    ) = Movement(
        id = id,
        type = type,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = "cat-1",
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = status,
        recordedAt = Instant.EPOCH,
        createdAt = Instant.EPOCH,
    )
    
    private fun commitment(
        id: String,
        targetAmount: String,
        currentAmount: String? = null,
        isSettled: Boolean,
    ) = Commitment(
        id = id,
        type = CommitmentType.SAVINGS_GOAL,
        title = "Title",
        targetAmount = Money.of(BigDecimal(targetAmount)).getOrError(),
        currentAmount = currentAmount?.let { Money.of(BigDecimal(it)).getOrError() },
        isSettled = isSettled,
    )
}
