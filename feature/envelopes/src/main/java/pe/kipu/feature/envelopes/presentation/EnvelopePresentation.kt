package pe.kipu.feature.envelopes.presentation

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuAmberDim
import pe.kipu.core.designsystem.theme.KipuBlue
import pe.kipu.core.designsystem.theme.KipuBlueDim
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuPrimaryDim
import pe.kipu.core.designsystem.theme.KipuPurple
import pe.kipu.core.designsystem.theme.KipuPurpleDim
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.designsystem.theme.KipuRedDim
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus

data class EnvelopeVisualStyle(
    val iconBackground: Color,
    val iconTint: Color,
    val progressColor: Color,
)

fun EnvelopeBudgetState.visualStyle(): EnvelopeVisualStyle = when (categoryId) {
    CategoryIds.FOOD -> EnvelopeVisualStyle(KipuAmberDim, KipuAmber, KipuAmber)
    CategoryIds.TRANSPORT -> EnvelopeVisualStyle(KipuBlueDim, KipuBlue, KipuBlue)
    CategoryIds.SERVICES -> EnvelopeVisualStyle(KipuPurpleDim, KipuPurple, KipuPurple)
    else -> when {
        name.contains("hormiga", ignoreCase = true) ->
            EnvelopeVisualStyle(KipuRedDim, KipuRed, KipuRed)
        name.contains("meta", ignoreCase = true) ->
            EnvelopeVisualStyle(KipuPrimaryDim, KipuPrimary, KipuPrimary)
        else -> EnvelopeVisualStyle(KipuPrimaryDim, KipuPrimary, KipuPrimary)
    }
}

fun EnvelopeBudgetState.percentLabel(): String = "${percentUsed.coerceAtLeast(0)}%"

fun EnvelopeBudgetState.percentToneColor(): Color = when {
    status == EnvelopeBudgetStatus.EXCEEDED || percentUsed >= 100 -> KipuRed
    status == EnvelopeBudgetStatus.ADJUSTED || percentUsed >= 75 -> KipuAmber
    else -> KipuPrimary
}

fun daysRemainingInCycle(cycle: BudgetCycle): Int {
    val today = LocalDate.now()
    val endOfCycle = when (cycle) {
        BudgetCycle.WEEKLY -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        BudgetCycle.MONTHLY -> today.with(TemporalAdjusters.lastDayOfMonth())
        BudgetCycle.DAILY -> today
    }
    return (endOfCycle.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
}

fun daysRemainingLabel(cycle: BudgetCycle): String {
    val days = daysRemainingInCycle(cycle)
    return when (days) {
        0 -> "Hoy"
        1 -> "1 día restante"
        else -> "$days días restantes"
    }
}
