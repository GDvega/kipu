package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError

class CalculateSavingsGoalProgressUseCaseTest {

    private val useCase = CalculateSavingsGoalProgressUseCase()

    @Test
    fun `includes linked income in progress calculation`() {
        val commitment = savingsGoal(
            target = "500.00",
            current = "100.00",
        )

        val result = useCase(
            commitment = commitment,
            linkedIncome = Money.of(BigDecimal("25.00")).getOrError(),
        )

        assertTrue(result is DomainResult.Ok)
        val progress = (result as DomainResult.Ok).value
        assertEquals(25, progress.progressPercent)
    }

    @Test
    fun `calculates 24 percent progress for known saved and target amounts`() {
        val commitment = savingsGoal(
            target = "500.00",
            current = "120.00",
        )

        val result = useCase(commitment)

        assertTrue(result is DomainResult.Ok)
        val progress = (result as DomainResult.Ok).value
        assertEquals(24, progress.progressPercent)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun `returns validation error when target is zero`() {
        val commitment = savingsGoal(
            target = "0.00",
            current = "10.00",
        )

        val result = useCase(commitment)

        assertTrue(result is DomainResult.Err)
    }

    @Test
    fun `caps progress at 100 percent and marks completed when saved exceeds target`() {
        val commitment = savingsGoal(
            target = "500.00",
            current = "600.00",
        )

        val result = useCase(commitment)

        assertTrue(result is DomainResult.Ok)
        val progress = (result as DomainResult.Ok).value
        assertEquals(100, progress.progressPercent)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `marks completed when settled even if progress is below 100 percent`() {
        val commitment = savingsGoal(
            target = "500.00",
            current = "120.00",
            isSettled = true,
        )

        val result = useCase(commitment)

        assertTrue(result is DomainResult.Ok)
        val progress = (result as DomainResult.Ok).value
        assertEquals(24, progress.progressPercent)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `rejects non savings goal commitment`() {
        val commitment = Commitment(
            id = "debt-1",
            type = CommitmentType.SOCIAL_DEBT,
            title = "Deuda",
            currentAmount = Money.of(BigDecimal("80.00")).getOrError(),
        )

        val result = useCase(commitment)

        assertTrue(result is DomainResult.Err)
    }

    private fun savingsGoal(
        target: String,
        current: String,
        isSettled: Boolean = false,
    ): Commitment = Commitment(
        id = "goal-1",
        type = CommitmentType.SAVINGS_GOAL,
        title = "Fondo emergencia",
        targetAmount = Money.of(BigDecimal(target)).getOrError(),
        currentAmount = Money.of(BigDecimal(current)).getOrError(),
        isSettled = isSettled,
    )
}
