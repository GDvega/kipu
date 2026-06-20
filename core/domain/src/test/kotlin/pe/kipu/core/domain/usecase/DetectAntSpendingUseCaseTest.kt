package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError

class DetectAntSpendingUseCaseTest {

    private val useCase = DetectAntSpendingUseCase()
    private val reference = Instant.parse("2026-06-17T15:00:00Z")

    @Test
    fun `detects several small expenses in 48 hour window`() {
        val movements = listOf(
            expense("m1", "5.00", CategoryIds.FOOD, hoursAgo = 10),
            expense("m2", "8.00", CategoryIds.FOOD, hoursAgo = 20),
            expense("m3", "12.00", CategoryIds.FOOD, hoursAgo = 30),
        )

        val alerts = useCase(movements, reference, isOverBudget = false)

        assertEquals(1, alerts.size)
        assertEquals(3, alerts.first().transactionCount)
        assertEquals(AlertSeverity.AMBER, alerts.first().severity)
        assertEquals(CategoryIds.FOOD, alerts.first().categoryId)
        assertEquals(Money.of(BigDecimal("25.00")).getOrError(), alerts.first().totalAmount)
    }

    @Test
    fun `does not alert on single small expense`() {
        val movements = listOf(
            expense("m1", "5.00", CategoryIds.FOOD, hoursAgo = 2),
        )

        val alerts = useCase(movements, reference, isOverBudget = false)

        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `ignores large expenses and non confirmed movements`() {
        val movements = listOf(
            expense("m1", "5.00", CategoryIds.FOOD, hoursAgo = 2),
            expense("m2", "6.00", CategoryIds.FOOD, hoursAgo = 4),
            expense("m3", "25.00", CategoryIds.FOOD, hoursAgo = 6),
            movement(
                id = "m4",
                amount = "4.00",
                categoryId = CategoryIds.FOOD,
                status = MovementStatus.PENDING_CONFIRMATION,
                hoursAgo = 8,
            ),
            movement(
                id = "m5",
                amount = "4.00",
                categoryId = CategoryIds.FOOD,
                type = MovementType.INCOME,
                hoursAgo = 10,
            ),
        )

        val alerts = useCase(movements, reference, isOverBudget = false)

        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `groups alerts by category`() {
        val movements = listOf(
            expense("m1", "5.00", CategoryIds.FOOD, hoursAgo = 1),
            expense("m2", "6.00", CategoryIds.FOOD, hoursAgo = 2),
            expense("m3", "7.00", CategoryIds.FOOD, hoursAgo = 3),
            expense("m4", "4.00", CategoryIds.TRANSPORT, hoursAgo = 4),
            expense("m5", "5.00", CategoryIds.TRANSPORT, hoursAgo = 5),
            expense("m6", "6.00", CategoryIds.TRANSPORT, hoursAgo = 6),
        )

        val alerts = useCase(movements, reference, isOverBudget = false)

        assertEquals(2, alerts.size)
        assertEquals(setOf(CategoryIds.FOOD, CategoryIds.TRANSPORT), alerts.map { it.categoryId }.toSet())
    }

    @Test
    fun `uses red severity when weekly budget is exceeded`() {
        val movements = listOf(
            expense("m1", "5.00", CategoryIds.FOOD, hoursAgo = 1),
            expense("m2", "6.00", CategoryIds.FOOD, hoursAgo = 2),
            expense("m3", "7.00", CategoryIds.FOOD, hoursAgo = 3),
        )

        val alerts = useCase(movements, reference, isOverBudget = true)

        assertEquals(AlertSeverity.RED, alerts.first().severity)
    }

    @Test
    fun `ignores movements outside 48 hour window`() {
        val movements = listOf(
            expense("m1", "5.00", CategoryIds.FOOD, hoursAgo = 50),
            expense("m2", "6.00", CategoryIds.FOOD, hoursAgo = 51),
            expense("m3", "7.00", CategoryIds.FOOD, hoursAgo = 52),
        )

        val alerts = useCase(movements, reference, isOverBudget = false)

        assertTrue(alerts.isEmpty())
    }

    private fun expense(
        id: String,
        amount: String,
        categoryId: String,
        hoursAgo: Long,
    ): Movement = movement(
        id = id,
        amount = amount,
        categoryId = categoryId,
        hoursAgo = hoursAgo,
    )

    private fun movement(
        id: String,
        amount: String,
        categoryId: String,
        hoursAgo: Long,
        type: MovementType = MovementType.EXPENSE,
        status: MovementStatus = MovementStatus.CONFIRMED,
    ): Movement {
        val recordedAt = reference.minus(hoursAgo, ChronoUnit.HOURS)
        return Movement(
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
}
