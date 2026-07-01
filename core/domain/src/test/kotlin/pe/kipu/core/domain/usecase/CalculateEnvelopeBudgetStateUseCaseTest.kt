package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.time.CycleRange
import pe.kipu.core.domain.time.CycleRangeCalculator

class CalculateEnvelopeBudgetStateUseCaseTest {

    private val useCase = CalculateEnvelopeBudgetStateUseCase(CalculateCategoryPeriodSpentUseCase())
    private val peruZone: ZoneId = CycleRangeCalculator.PERU_ZONE
    private val cycleRange = CycleRange(
        start = ZonedDateTime.of(2026, 6, 15, 0, 0, 0, 0, peruZone).toInstant(),
        end = ZonedDateTime.of(2026, 6, 22, 0, 0, 0, 0, peruZone).toInstant(),
    )

    private val foodEnvelope = Envelope(
        id = "envelope-food",
        name = "Comida",
        weeklyLimit = Money.of(BigDecimal("150.00")).getOrError(),
        categoryId = CategoryIds.FOOD,
    )

    @Test
    fun `handles envelope with zero spending`() {
        val state = useCase(foodEnvelope, emptyList(), cycleRange)

        assertEquals(Money.ZERO, state.spentAmount)
        assertEquals(0, state.percentUsed)
        assertEquals(EnvelopeBudgetStatus.OK, state.status)
        assertEquals(BigDecimal("150.00"), state.remainingAmount.amount)
    }

    @Test
    fun `calculates percent used`() {
        val movements = listOf(
            expense(CategoryIds.FOOD, "120.00", instant(2026, 6, 16, 10, 0)),
        )

        val state = useCase(foodEnvelope, movements, cycleRange)

        assertEquals(80, state.percentUsed)
        assertEquals(EnvelopeBudgetStatus.ADJUSTED, state.status)
    }

    @Test
    fun `detects adjusted envelope near limit`() {
        val movements = listOf(
            expense(CategoryIds.FOOD, "121.00", instant(2026, 6, 16, 10, 0)),
        )

        val state = useCase(foodEnvelope, movements, cycleRange)

        assertEquals(EnvelopeBudgetStatus.ADJUSTED, state.status)
    }

    @Test
    fun `detects exceeded envelope`() {
        val movements = listOf(
            expense(CategoryIds.FOOD, "160.00", instant(2026, 6, 16, 10, 0)),
        )

        val state = useCase(foodEnvelope, movements, cycleRange)

        assertEquals(EnvelopeBudgetStatus.EXCEEDED, state.status)
        assertEquals(Money.ZERO, state.remainingAmount)
        assertEquals(107, state.percentUsed)
    }

    @Test
    fun `rejects zero envelope limit`() {
        val invalid = foodEnvelope.copy(weeklyLimit = Money.ZERO)
        assertTrue(invalid.validate() is pe.kipu.core.domain.model.DomainResult.Err)
    }

    @Test
    fun `excludes gathering linked movements from budget spent calculation`() {
        val m1 = expense(CategoryIds.FOOD, "120.00", instant(2026, 6, 16, 10, 0))
        val m2 = expense(CategoryIds.FOOD, "40.00", instant(2026, 6, 16, 11, 0))
        
        // Without exclusion, spent = 160 -> EXCEEDED
        // With m1 excluded (linked to gathering), spent = 40 -> OK
        val state = useCase(
            envelope = foodEnvelope,
            movements = listOf(m1, m2),
            cycleRange = cycleRange,
            gatheringLinkedMovementIds = setOf(m1.id),
        )

        assertEquals(EnvelopeBudgetStatus.OK, state.status)
        assertEquals(BigDecimal("40.00"), state.spentAmount.amount)
        assertEquals(27, state.percentUsed)
    }

    private fun expense(categoryId: String, amount: String, recordedAt: Instant) = Movement(
        id = "movement-$amount",
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, peruZone).toInstant()
}
