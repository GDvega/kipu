package pe.kipu.feature.envelopes.presentation

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
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

fun EnvelopeBudgetState.percentToneColor(): Color = when (status) {
    EnvelopeBudgetStatus.OK -> KipuPrimary
    EnvelopeBudgetStatus.ADJUSTED -> KipuAmber
    EnvelopeBudgetStatus.EXCEEDED -> KipuRed
}

fun daysRemainingInWeek(): Int {
    val today = LocalDate.now()
    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return (endOfWeek.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
}

fun daysRemainingLabel(): String {
    val days = daysRemainingInWeek()
    return when (days) {
        0 -> "Hoy"
        1 -> "1 día"
        else -> "$days días"
    }
}
