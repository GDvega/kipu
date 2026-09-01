package pe.kipu.core.domain.export

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey

class UserDataJsonSerializerTest {

    private val serializer = UserDataJsonSerializer()
    private val instant = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `serializes the complete persisted snapshot`() {
        val snapshot = UserDataSnapshot(
            exportedAt = instant,
            movements = listOf(sampleMovement()),
            categories = listOf(Category(id = "category-food", name = "Comida")),
            envelopes = emptyList(),
            commitments = emptyList(),
            financialPlans = listOf(
                FinancialPlan(
                    id = "plan-1",
                    estimatedMonthlyIncome = money("2500.00"),
                    fixedExpenses = money("900.00"),
                    initialBalance = money("350.00"),
                    reserveMonthlyContribution = money("100.00"),
                    envelopeIds = listOf("envelope-1"),
                    incomeProfile = IncomeProfile.VARIABLE,
                    payFrequency = PayFrequency.BIWEEKLY,
                    budgetCycle = BudgetCycle.MONTHLY,
                    antSpendingLimit = money("120.00"),
                    antSpendingAlertEnabled = false,
                    antSpendingAlertPercent = 75,
                    antSpendingTrackedCategoryIds = setOf("category-food"),
                    electricityExpenses = money("45.00"),
                    customFixedExpensesJson = "[{\"label\":\"Gas\"}]",
                ),
            ),
            gatherings = listOf(
                Gathering(
                    id = "gathering-1",
                    name = "Viaje",
                    participantCount = 2,
                    participantNames = listOf("María", "Luis"),
                    isSettled = true,
                ),
            ),
            gatheringExpenses = listOf(
                GatheringExpense(
                    id = "expense-1",
                    gatheringId = "gathering-1",
                    amount = money("60.00"),
                    paidByParticipant = "María",
                    description = "Cena",
                    movementId = "movement-1",
                    recordedAt = instant,
                ),
            ),
            dismissedDuplicatePairKeys = setOf("pair-1"),
            preferences = UserPreferences(
                themeMode = ThemeMode.DARK,
                notificationsEnabled = true,
                onboardingCompleted = true,
                widgetDailyAvailableUpdatedAtMillis = 1_786_122_600_000L,
                budgetCycle = BudgetCycle.DAILY,
            ),
            monthlyServiceReceipts = listOf(
                MonthlyServiceReceipt(
                    key = ServiceReceiptKey.LIGHT,
                    title = "Luz",
                    configuredAmount = money("60.00"),
                    monthKey = "2026-06",
                    isPaid = true,
                    paidMovementId = "movement-1",
                    paidAt = instant,
                    paidAmount = money("55.00"),
                ),
            ),
            movementAuditEntries = listOf(
                MovementAuditEntry(
                    id = "audit-1",
                    movementId = "movement-1",
                    action = MovementAuditAction.CREATED,
                    movementType = MovementType.EXPENSE,
                    amount = money("25.50"),
                    categoryId = "category-food",
                    channel = PaymentChannel.YAPE,
                    timestamp = instant,
                ),
            ),
            reserveEvents = listOf(
                ReserveEvent(
                    id = "reserve-use-1",
                    type = ReserveEventType.USE,
                    amount = money("80.00"),
                    sourceMovementId = "movement-1",
                    occurredAt = instant,
                    createdAt = instant,
                ),
            ),
        )

        val json = serializer.serialize(snapshot)

        assertTrue(json.contains("\"exportVersion\":5"))
        assertTrue(json.contains("\"counterpartyName\":\"María\""))
        assertTrue(json.contains("\"themeMode\":\"DARK\""))
        assertTrue(json.contains("\"dismissedDuplicatePairKeys\":[\"pair-1\"]"))
        assertTrue(json.contains("\"gatheringExpenses\":[{\"id\":\"expense-1\""))
        assertTrue(json.contains("\"incomeProfile\":\"VARIABLE\""))
        assertTrue(json.contains("\"payFrequency\":\"BIWEEKLY\""))
        assertTrue(json.contains("\"budgetCycle\":\"MONTHLY\""))
        assertTrue(json.contains("\"antSpendingLimit\":\"120.00\""))
        assertTrue(json.contains("\"isSettled\":true"))
        assertTrue(json.contains("\"budgetCycle\":\"DAILY\""))
        assertTrue(json.contains("\"widgetDailyAvailableUpdatedAtMillis\":1786122600000"))
        assertTrue(json.contains("\"monthlyServiceReceipts\":[{\"key\":\"LIGHT\""))
        assertTrue(json.contains("\"movementAuditEntries\":[{\"id\":\"audit-1\""))
        assertTrue(json.contains("\"envelopeId\":\"envelope-1\""))
        assertTrue(json.contains("\"reserveMonthlyContribution\":\"100.00\""))
        assertTrue(json.contains("\"electricityExpenses\":\"45.00\""))
        assertTrue(json.contains("\"customFixedExpensesJson\":\"[{\\\"label\\\":\\\"Gas\\\"}]\""))
        assertTrue(json.contains("\"paidAmount\":\"55.00\""))
        assertTrue(json.contains("\"reserveEvents\":[{\"id\":\"reserve-use-1\""))
    }

    private fun sampleMovement(): Movement = Movement(
        id = "movement-1",
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.50")).getOrError(),
        categoryId = "category-food",
        channel = PaymentChannel.YAPE,
        source = MovementSource.RECEIPT,
        status = MovementStatus.CONFIRMED,
        counterpartyName = "María",
        envelopeId = "envelope-1",
        recordedAt = instant,
        createdAt = instant,
    )

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()
}
