package pe.kipu.feature.home.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.AntSpendingAlertKeys
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
}
