package pe.kipu.feature.home.presentation

import pe.kipu.core.domain.model.BudgetCycle

object HomeCycleText {
    fun periodTitle(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "Hoy"
        BudgetCycle.WEEKLY -> "Esta semana"
        BudgetCycle.MONTHLY -> "Este mes"
    }

    fun periodPhrase(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "hoy"
        BudgetCycle.WEEKLY -> "esta semana"
        BudgetCycle.MONTHLY -> "este mes"
    }

    fun overBudgetContentDescription(cycle: BudgetCycle): String =
        "Presupuesto ${adjective(cycle)} excedido"

    fun overBudgetMessage(cycle: BudgetCycle): String =
        "Ya pasaste tu presupuesto ${adjective(cycle)}"

    fun noDaysRemaining(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "El presupuesto de hoy terminó"
        BudgetCycle.WEEKLY -> "Sin días restantes esta semana"
        BudgetCycle.MONTHLY -> "Sin días restantes este mes"
    }

    fun remainingDays(cycle: BudgetCycle, days: Int): String {
        val verb = if (days == 1) "Te queda" else "Te quedan"
        val unit = if (days == 1) "día" else "días"
        return "$verb $days $unit ${periodPhrase(cycle)}"
    }

    private fun adjective(cycle: BudgetCycle): String = when (cycle) {
        BudgetCycle.DAILY -> "diario"
        BudgetCycle.WEEKLY -> "semanal"
        BudgetCycle.MONTHLY -> "mensual"
    }
}
