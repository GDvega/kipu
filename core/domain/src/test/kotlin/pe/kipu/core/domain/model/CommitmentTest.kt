package pe.kipu.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CommitmentTest {

    @Test
    fun validate_acceptsOpenSavingsGoal() {
        val commitment = Commitment(
            id = "commitment-1",
            type = CommitmentType.SAVINGS_GOAL,
            title = "Fondo emergencia",
            targetAmount = Money.of(BigDecimal("1000.00")).getOrError(),
            isSettled = false,
        )
        assertTrue(commitment.validate() is DomainResult.Ok)
        assertFalse(commitment.isSettled)
    }

    @Test
    fun validate_acceptsSettledSocialDebt() {
        val commitment = Commitment(
            id = "commitment-2",
            type = CommitmentType.SOCIAL_DEBT,
            title = "Deuda con Juan",
            currentAmount = Money.of(BigDecimal("50.00")).getOrError(),
            isSettled = true,
        )
        assertTrue(commitment.validate() is DomainResult.Ok)
    }

    @Test
    fun validate_rejectsSettledPendingPaymentWithoutCurrentAmount() {
        val commitment = Commitment(
            id = "commitment-3",
            type = CommitmentType.PENDING_PAYMENT,
            title = "Pago alquiler",
            isSettled = true,
        )
        assertTrue(commitment.validate() is DomainResult.Err)
    }
}
