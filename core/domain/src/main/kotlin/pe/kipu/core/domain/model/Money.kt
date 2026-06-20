package pe.kipu.core.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Immutable amount in Peruvian soles (PEN).
 *
 * Uses [BigDecimal] for fractional pesos (centavos) with scale 2.
 * Amounts are always non-negative; direction is expressed via [MovementType].
 */
@ConsistentCopyVisibility
data class Money private constructor(
    val amount: BigDecimal,
) {
    init {
        require(amount.scale() <= SCALE) {
            "Money supports at most $SCALE decimal places"
        }
        require(amount >= BigDecimal.ZERO) {
            "Money amount must be non-negative"
        }
    }

    operator fun plus(other: Money): Money = Money(
        (amount + other.amount).setScale(SCALE, RoundingMode.HALF_UP),
    )

    operator fun minus(other: Money): DomainResult<Money> {
        val result = amount.subtract(other.amount)
        return if (result < BigDecimal.ZERO) {
            DomainResult.Err(DomainError.InvalidAmount("Subtraction would yield negative money"))
        } else {
            DomainResult.Ok(Money(result.setScale(SCALE, RoundingMode.HALF_UP)))
        }
    }

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    companion object {
        const val CURRENCY_CODE = "PEN"
        private const val SCALE = 2

        val ZERO: Money = Money(BigDecimal.ZERO.setScale(SCALE))

        fun of(amount: BigDecimal): DomainResult<Money> {
            if (amount < BigDecimal.ZERO) {
                return DomainResult.Err(DomainError.InvalidAmount("Amount must be non-negative"))
            }
            return DomainResult.Ok(Money(amount.setScale(SCALE, RoundingMode.HALF_UP)))
        }

        fun ofPesos(whole: Long, cents: Int = 0): DomainResult<Money> {
            if (cents !in 0..99) {
                return DomainResult.Err(DomainError.InvalidAmount("Cents must be between 0 and 99"))
            }
            val value = BigDecimal.valueOf(whole).add(
                BigDecimal.valueOf(cents.toLong()).movePointLeft(2),
            )
            return of(value)
        }
    }
}
