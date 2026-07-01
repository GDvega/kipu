package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import java.time.Instant

class CommitmentLinkedIncomeCalculatorTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `sums only confirmed income linked to commitment`() {
        val movements = listOf(
            linkedIncome("mov-1", "goal-1", "80.00"),
            linkedIncome("mov-2", "goal-1", "40.00"),
            linkedIncome("mov-3", "goal-2", "200.00"),
            linkedIncome("mov-4", "goal-1", "10.00").copy(status = MovementStatus.PENDING_CONFIRMATION),
            linkedIncome("mov-5", "goal-1", "15.00").copy(type = MovementType.EXPENSE),
        )

        val total = CommitmentLinkedIncomeCalculator.sumLinkedIncome("goal-1", movements)

        assertEquals(BigDecimal("120.00"), total.amount)
    }

    private fun linkedIncome(
        id: String,
        goalId: String,
        amount: String,
    ): Movement = Movement(
        id = id,
        type = MovementType.INCOME,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = "category-other",
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        commitmentId = goalId,
        recordedAt = now,
        createdAt = now,
    )
}
