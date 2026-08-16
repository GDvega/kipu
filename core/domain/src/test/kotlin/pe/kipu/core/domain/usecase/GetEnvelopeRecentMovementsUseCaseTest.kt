package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator

class GetEnvelopeRecentMovementsUseCaseTest {

    private val reference = Instant.parse("2026-06-16T15:00:00Z")

    @Test
    fun `returns confirmed expenses for category in current week sorted by date`() {
        val inWeekRecent = movement(
            id = "m1",
            categoryId = CategoryIds.FOOD,
            recordedAt = Instant.parse("2026-06-16T10:00:00Z"),
        )
        val inWeekOlder = movement(
            id = "m2",
            categoryId = CategoryIds.FOOD,
            recordedAt = Instant.parse("2026-06-16T08:00:00Z"),
        )
        val otherCategory = movement(
            id = "m3",
            categoryId = CategoryIds.TRANSPORT,
            recordedAt = Instant.parse("2026-06-16T11:00:00Z"),
        )
        val pending = movement(
            id = "m4",
            categoryId = CategoryIds.FOOD,
            status = MovementStatus.PENDING_CONFIRMATION,
            recordedAt = Instant.parse("2026-06-16T12:00:00Z"),
        )
        val useCase = GetEnvelopeRecentMovementsUseCase(
            cycleRangeCalculator = CycleRangeCalculator(FixedTimeProvider(reference)),
            timeProvider = FixedTimeProvider(reference),
        )

        val result = useCase(
            categoryId = CategoryIds.FOOD,
            movements = listOf(inWeekRecent, inWeekOlder, otherCategory, pending),
            cycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
            limit = 3,
        )

        assertEquals(listOf("m1", "m2"), result.map { it.id })
    }

    @Test
    fun `monthly cycle includes expenses from earlier in the month`() {
        val inWeek = movement(
            id = "m1",
            categoryId = CategoryIds.FOOD,
            recordedAt = Instant.parse("2026-06-16T10:00:00Z"),
        )
        // 2026-06-03 está en el mes pero fuera de la semana del 15-21.
        val earlierInMonth = movement(
            id = "m2",
            categoryId = CategoryIds.FOOD,
            recordedAt = Instant.parse("2026-06-03T10:00:00Z"),
        )
        val useCase = GetEnvelopeRecentMovementsUseCase(
            cycleRangeCalculator = CycleRangeCalculator(FixedTimeProvider(reference)),
            timeProvider = FixedTimeProvider(reference),
        )

        val weekly = useCase(
            categoryId = CategoryIds.FOOD,
            movements = listOf(inWeek, earlierInMonth),
            cycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
        )
        val monthly = useCase(
            categoryId = CategoryIds.FOOD,
            movements = listOf(inWeek, earlierInMonth),
            cycle = pe.kipu.core.domain.model.BudgetCycle.MONTHLY,
        )

        assertEquals(listOf("m1"), weekly.map { it.id })
        assertEquals(listOf("m1", "m2"), monthly.map { it.id })
    }

    private fun movement(
        id: String,
        categoryId: String,
        recordedAt: Instant,
        status: MovementStatus = MovementStatus.CONFIRMED,
    ): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("15.00")).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.YAPE,
        source = MovementSource.RECEIPT,
        status = status,
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )

    private class FixedTimeProvider(
        private val now: Instant,
    ) : TimeProvider {
        override fun now(): Instant = now
    }
}
