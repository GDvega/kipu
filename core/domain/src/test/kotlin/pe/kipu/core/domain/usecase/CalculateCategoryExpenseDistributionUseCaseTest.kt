package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.time.CycleRange
import java.math.BigDecimal
import java.time.Instant

class CalculateCategoryExpenseDistributionUseCaseTest {

    private val useCase = CalculateCategoryExpenseDistributionUseCase()

    private val now = Instant.parse("2026-08-22T12:00:00Z")
    private val cycleRange = CycleRange(
        start = Instant.parse("2026-08-17T00:00:00Z"),
        end = Instant.parse("2026-08-24T00:00:00Z"),
    )

    private val categories = listOf(
        Category(id = "cat-food", name = "Alimentación", iconKey = "food"),
        Category(id = "cat-services", name = "Servicios", iconKey = "services"),
        Category(id = "cat-transport", name = "Transporte", iconKey = "bus"),
    )

    @Test
    fun `empty movements returns empty distribution`() {
        val result = useCase(
            movements = emptyList(),
            categories = categories,
            cycleRange = cycleRange,
        )

        assertEquals(Money.ZERO, result.totalSpent)
        assertTrue(result.slices.isEmpty())
        assertTrue(result.isEmpty)
    }

    @Test
    fun `ignores income and unconfirmed movements`() {
        val movements = listOf(
            Movement(
                id = "mov-1",
                type = MovementType.INCOME,
                amount = Money.of(BigDecimal("500.00")).getOrError(),
                categoryId = "cat-food",
                channel = PaymentChannel.YAPE,
                source = MovementSource.NOTIFICATION,
                status = MovementStatus.CONFIRMED,
                recordedAt = now,
                createdAt = now,
            ),
            Movement(
                id = "mov-2",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("30.00")).getOrError(),
                categoryId = "cat-food",
                channel = PaymentChannel.YAPE,
                source = MovementSource.RECEIPT,
                status = MovementStatus.PENDING_CONFIRMATION,
                recordedAt = now,
                createdAt = now,
            ),
        )

        val result = useCase(
            movements = movements,
            categories = categories,
            cycleRange = cycleRange,
        )

        assertEquals(Money.ZERO, result.totalSpent)
        assertTrue(result.slices.isEmpty())
    }

    @Test
    fun `ignores movements outside cycle range`() {
        val pastInstant = Instant.parse("2026-08-10T12:00:00Z")
        val movements = listOf(
            Movement(
                id = "mov-old",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("40.00")).getOrError(),
                categoryId = "cat-food",
                channel = PaymentChannel.YAPE,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                recordedAt = pastInstant,
                createdAt = pastInstant,
            ),
        )

        val result = useCase(
            movements = movements,
            categories = categories,
            cycleRange = cycleRange,
        )

        assertEquals(Money.ZERO, result.totalSpent)
        assertTrue(result.slices.isEmpty())
    }

    @Test
    fun `calculates single category distribution with 100 percent`() {
        val movements = listOf(
            Movement(
                id = "mov-1",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("25.00")).getOrError(),
                categoryId = "cat-food",
                channel = PaymentChannel.YAPE,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                recordedAt = now,
                createdAt = now,
            ),
            Movement(
                id = "mov-2",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("75.00")).getOrError(),
                categoryId = "cat-food",
                channel = PaymentChannel.PLIN,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                recordedAt = now,
                createdAt = now,
            ),
        )

        val result = useCase(
            movements = movements,
            categories = categories,
            cycleRange = cycleRange,
        )

        assertEquals(Money.of(BigDecimal("100.00")).getOrError(), result.totalSpent)
        assertEquals(1, result.slices.size)
        assertEquals(2, result.totalTransactions)

        val slice = result.slices.first()
        assertEquals("cat-food", slice.categoryId)
        assertEquals("Alimentación", slice.categoryName)
        assertEquals(Money.of(BigDecimal("100.00")).getOrError(), slice.totalAmount)
        assertEquals(1.0f, slice.percentage, 0.001f)
        assertEquals("100%", slice.percentageFormatted)
        assertEquals(2, slice.transactionCount)
        assertEquals(0, slice.colorIndex)

        assertEquals(slice, result.topCategory)
    }

    @Test
    fun `calculates multiple categories sorted descending by amount with correct percentages`() {
        val movements = listOf(
            Movement(
                id = "mov-1",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("100.00")).getOrError(), // 50%
                categoryId = "cat-food",
                channel = PaymentChannel.YAPE,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                recordedAt = now,
                createdAt = now,
            ),
            Movement(
                id = "mov-2",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("60.00")).getOrError(), // 30%
                categoryId = "cat-services",
                channel = PaymentChannel.CASH,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                recordedAt = now,
                createdAt = now,
            ),
            Movement(
                id = "mov-3",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("40.00")).getOrError(), // 20%
                categoryId = "cat-transport",
                channel = PaymentChannel.PLIN,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                recordedAt = now,
                createdAt = now,
            ),
        )

        val result = useCase(
            movements = movements,
            categories = categories,
            cycleRange = cycleRange,
        )

        assertEquals(Money.of(BigDecimal("200.00")).getOrError(), result.totalSpent)
        assertEquals(3, result.slices.size)
        assertEquals(3, result.totalTransactions)

        // Slices sorted descending: Food (100), Services (60), Transport (40)
        val slice1 = result.slices[0]
        assertEquals("cat-food", slice1.categoryId)
        assertEquals("Alimentación", slice1.categoryName)
        assertEquals(0.50f, slice1.percentage, 0.001f)
        assertEquals("50%", slice1.percentageFormatted)
        assertEquals(0, slice1.colorIndex)

        val slice2 = result.slices[1]
        assertEquals("cat-services", slice2.categoryId)
        assertEquals("Servicios", slice2.categoryName)
        assertEquals(0.30f, slice2.percentage, 0.001f)
        assertEquals("30%", slice2.percentageFormatted)
        assertEquals(1, slice2.colorIndex)

        val slice3 = result.slices[2]
        assertEquals("cat-transport", slice3.categoryId)
        assertEquals("Transporte", slice3.categoryName)
        assertEquals(0.20f, slice3.percentage, 0.001f)
        assertEquals("20%", slice3.percentageFormatted)
        assertEquals(2, slice3.colorIndex)

        assertEquals(slice1, result.topCategory)
    }
}
