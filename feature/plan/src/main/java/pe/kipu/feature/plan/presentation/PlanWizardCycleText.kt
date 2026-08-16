package pe.kipu.feature.plan.presentation

import pe.kipu.core.domain.model.BudgetCycle

object PlanWizardCycleText {
    fun antSpendingLimitTitle(cycle: BudgetCycle): String =
        "Límite ${adjective(cycle)} de gastos hormiga"

    fun antSpendingSubtitle(cycle: BudgetCycle): String =
        "El cafecito, la gaseosa, el snack... ¿cuánto se te escapa ${periodPreposition(cycle)}?"

    fun envelopeTitle(cycle: BudgetCycle): String =
        "Tus sobres ${pluralAdjective(cycle)}"

    fun envelopeLimitSuffix(cycle: BudgetCycle): String = periodPreposition(cycle)

    fun budgetDescription(cycle: BudgetCycle): String =
        "Sin descuadrarte de tu presupuesto ${adjective(cycle)}"

    fun reduceEnvelopesAction(cycle: BudgetCycle): String =
        "Reducir sobres ${pluralAdjective(cycle)}"

    private fun periodPreposition(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "por día"
        BudgetCycle.WEEKLY -> "por semana"
        BudgetCycle.MONTHLY -> "por mes"
    }

    private fun adjective(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "diario"
        BudgetCycle.WEEKLY -> "semanal"
        BudgetCycle.MONTHLY -> "mensual"
    }

    private fun pluralAdjective(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "diarios"
        BudgetCycle.WEEKLY -> "semanales"
        BudgetCycle.MONTHLY -> "mensuales"
    }
}
