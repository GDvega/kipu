package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.time.CycleRange
import pe.kipu.core.domain.time.CycleRangeCalculator

class CalculateCategoryPeriodSpentUseCaseTest {

    private val useCase = CalculateCategoryPeriodSpentUseCase()
    private val peruZone: ZoneId = CycleRangeCalculator.PERU_ZONE
    private val cycleRange = CycleRange(
        start = ZonedDateTime.of(2026, 6, 15, 0, 0, 0, 0, peruZone).toInstant(),
        end = ZonedDateTime.of(2026, 6, 22, 0, 0, 0, 0, peruZone).toInstant(),
    )

    @Test
    fun `sums only confirmed expenses for category in week range`() {
        val movements = listOf(
            movement(
                id = "m1",
                categoryId = CategoryIds.FOOD,
                amount = "25.00",
                recordedAt = instant(2026, 6, 16, 10, 0),
                type = MovementType.EXPENSE,
                status = MovementStatus.CONFIRMED,
            ),
            movement(
                id = "m2",
                categoryId = CategoryIds.FOOD,
                amount = "10.50",
                recordedAt = instant(2026, 6, 17, 18, 0),
            ),
            movement(
                id = "m3",
                categoryId = CategoryIds.TRANSPORT,
                amount = "5.00",
                recordedAt = instant(2026, 6, 16, 11, 0),
            ),
            movement(
                id = "m4",
                categoryId = CategoryIds.FOOD,
                amount = "99.00",
                recordedAt = instant(2026, 6, 14, 23, 0),
            ),
            movement(
                id = "m5",
                categoryId = CategoryIds.FOOD,
                amount = "15.00",
                recordedAt = instant(2026, 6, 16, 12, 0),
                type = MovementType.INCOME,
            ),
            movement(
                id = "m6",
                categoryId = CategoryIds.FOOD,
                amount = "8.00",
                recordedAt = instant(2026, 6, 16, 13, 0),
                status = MovementStatus.PENDING_CONFIRMATION,
            ),
        )

        val total = useCase(CategoryIds.FOOD, movements, cycleRange)

        assertEquals(BigDecimal("35.50"), total.amount)
    }

    @Test
    fun `returns zero when no matching movements`() {
        val total = useCase(CategoryIds.FOOD, emptyList(), cycleRange)

        assertEquals(Money.ZERO, total)
    }

    @Test
    fun `excludes movements linked to active gatherings`() {
        val movements = listOf(
            movement(
                id = "m1",
                categoryId = CategoryIds.FOOD,
                amount = "25.00",
                recordedAt = instant(2026, 6, 16, 10, 0),
            ),
            movement(
                id = "m2",
                categoryId = CategoryIds.FOOD,
                amount = "150.00",
                recordedAt = instant(2026, 6, 16, 12, 0),
            ),
        )

        // m2 is linked to an active gathering
        val total = useCase(
            categoryId = CategoryIds.FOOD,
            movements = movements,
            cycleRange = cycleRange,
            gatheringLinkedMovementIds = setOf("m2"),
        )

        assertEquals(BigDecimal("25.00"), total.amount)
    }

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, peruZone).toInstant()

    private fun movement(
        id: String,
        categoryId: String,
        amount: String,
        recordedAt: Instant,
        type: MovementType = MovementType.EXPENSE,
        status: MovementStatus = MovementStatus.CONFIRMED,
    ) = Movement(
        id = id,
        type = type,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = status,
        recordedAt = recordedAt,
        createdAt = recordedAt,
    )
}
