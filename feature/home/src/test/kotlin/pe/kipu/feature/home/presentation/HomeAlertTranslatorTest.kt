package pe.kipu.feature.home.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.AntSpendingAlertKeys
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class HomeAlertTranslatorTest {

    @Test
    fun `includes category name when available`() {
        val alert = AntSpendingAlert(
            severity = AlertSeverity.AMBER,
            transactionCount = 3,
            totalAmount = Money.of(BigDecimal("18.00")).getOrError(),
            windowHours = 48,
            categoryId = "category-food",
            messageKey = AntSpendingAlertKeys.CATEGORY,
        )

        val text = HomeAlertTranslator.toDisplayText(alert, categoryName = "Comida")

        assertEquals(
            "Llevas 3 gastos pequeños por S/ 18.00 en las últimas 48 horas en Comida.",
            text,
        )
    }

    @Test
    fun `falls back when category name is missing`() {
        val alert = AntSpendingAlert(
            severity = AlertSeverity.AMBER,
            transactionCount = 3,
            totalAmount = Money.of(BigDecimal("18.00")).getOrError(),
            windowHours = 48,
            categoryId = null,
            messageKey = AntSpendingAlertKeys.CATEGORY,
        )

        val text = HomeAlertTranslator.toDisplayText(alert, categoryName = null)

        assertEquals(
            "Llevas 3 gastos pequeños por S/ 18.00 en las últimas 48 horas en esta categoría.",
            text,
        )
    }

    @Test
    fun `uses monthly period for cycle limit alert`() {
        val alert = AntSpendingAlert(
            severity = AlertSeverity.RED,
            transactionCount = 6,
            totalAmount = Money.of(BigDecimal("80.00")).getOrError(),
            windowHours = 48,
            categoryId = null,
            messageKey = AntSpendingAlertKeys.WEEKLY_LIMIT,
        )

        val text = HomeAlertTranslator.toDisplayText(
            alert = alert,
            cycle = BudgetCycle.MONTHLY,
        )

        assertEquals(
            "Llevas S/ 80.00 en gastos hormiga este mes.",
            text,
        )
    }

    @Test
    fun `provides consistent home copy for every budget cycle`() {
        assertEquals("Hoy", HomeCycleText.periodTitle(BudgetCycle.DAILY))
        assertEquals("Esta semana", HomeCycleText.periodTitle(BudgetCycle.WEEKLY))
        assertEquals("Este mes", HomeCycleText.periodTitle(BudgetCycle.MONTHLY))
        assertEquals(
            "Presupuesto mensual excedido",
            HomeCycleText.overBudgetContentDescription(BudgetCycle.MONTHLY),
        )
        assertEquals(
            "Te quedan 4 días este mes",
            HomeCycleText.remainingDays(BudgetCycle.MONTHLY, days = 4),
        )
    }
}
