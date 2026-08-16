package pe.kipu.feature.home.presentation

import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.AntSpendingAlertKeys
import pe.kipu.core.domain.model.BudgetCycle

object HomeAlertTranslator {

    fun toDisplayText(
        alert: AntSpendingAlert,
        categoryName: String? = null,
        cycle: BudgetCycle = BudgetCycle.WEEKLY,
    ): String = when (alert.messageKey) {
        AntSpendingAlertKeys.CATEGORY -> {
            val categoryLabel = categoryName?.takeIf { it.isNotBlank() } ?: "esta categoría"
            "Llevas ${alert.transactionCount} gastos pequeños por " +
                "${formatPenAmountForDisplay(alert.totalAmount.amount)} en las últimas " +
                "${alert.windowHours} horas en $categoryLabel."
        }

        AntSpendingAlertKeys.WEEKLY_LIMIT ->
            "Llevas ${formatPenAmountForDisplay(alert.totalAmount.amount)} en gastos hormiga " +
                "${HomeCycleText.periodPhrase(cycle)}."

        else ->
            "Detectamos varios gastos pequeños en las últimas ${alert.windowHours} horas."
    }
}
