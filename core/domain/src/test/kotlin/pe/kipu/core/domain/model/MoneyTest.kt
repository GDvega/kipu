package pe.kipu.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun of_acceptsZero() {
        val result = Money.of(BigDecimal.ZERO)
        assertTrue(result is DomainResult.Ok)
        assertEquals(BigDecimal("0.00"), (result as DomainResult.Ok).value.amount)
    }

    @Test
    fun of_acceptsPositiveAmountWithRounding() {
        val result = Money.of(BigDecimal("10.567"))
        assertTrue(result is DomainResult.Ok)
        assertEquals(BigDecimal("10.57"), (result as DomainResult.Ok).value.amount)
    }

    @Test
    fun of_rejectsNegativeAmount() {
        val result = Money.of(BigDecimal("-0.01"))
        assertTrue(result is DomainResult.Err)
        assertTrue((result as DomainResult.Err).error is DomainError.InvalidAmount)
    }

    @Test
    fun ofPesos_buildsAmountFromWholeAndCents() {
        val result = Money.ofPesos(whole = 25, cents = 50)
        assertTrue(result is DomainResult.Ok)
        assertEquals(BigDecimal("25.50"), (result as DomainResult.Ok).value.amount)
    }

    @Test
    fun ofPesos_rejectsInvalidCents() {
        val result = Money.ofPesos(whole = 1, cents = 100)
        assertTrue(result is DomainResult.Err)
    }

    @Test
    fun plus_sumsTwoAmounts() {
        val left = Money.of(BigDecimal("10.25")).getOrError()
        val right = Money.of(BigDecimal("2.75")).getOrError()
        assertEquals(BigDecimal("13.00"), (left + right).amount)
    }

    @Test
    fun minus_returnsErrorWhenResultWouldBeNegative() {
        val left = Money.of(BigDecimal("5.00")).getOrError()
        val right = Money.of(BigDecimal("5.01")).getOrError()
        val result = left - right
        assertTrue(result is DomainResult.Err)
    }

    @Test
    fun minus_subtractsWhenEnoughBalance() {
        val left = Money.of(BigDecimal("10.00")).getOrError()
        val right = Money.of(BigDecimal("3.50")).getOrError()
        val result = left - right
        assertTrue(result is DomainResult.Ok)
        assertEquals(BigDecimal("6.50"), (result as DomainResult.Ok).value.amount)
    }

    @Test
    fun isZero_detectsZeroAmount() {
        assertTrue(Money.ZERO.isZero())
        assertFalse(Money.of(BigDecimal.ONE).getOrError().isZero())
    }

    @Test
    fun currencyCode_isPen() {
        assertEquals("PEN", Money.CURRENCY_CODE)
    }
}
