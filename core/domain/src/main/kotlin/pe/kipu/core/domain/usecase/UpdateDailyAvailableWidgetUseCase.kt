package pe.kipu.core.domain.usecase

import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.HomeInsights
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.widget.DailyAvailableWidgetGateway

/**
 * Persists a display-only snapshot for the "Disponible hoy" widget and requests a refresh.
 */
class UpdateDailyAvailableWidgetUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val widgetGateway: DailyAvailableWidgetGateway,
) {

    suspend operator fun invoke(insights: HomeInsights): Result<Unit> {
        val displayText = resolveDisplayText(insights)
        val isOverBudget = insights.dailyAvailable.isOverBudget

        return userPreferencesRepository.updatePreferences { prefs ->
            prefs.copy(
                widgetDailyAvailableText = displayText,
                widgetIsOverBudget = isOverBudget,
            )
        }.also { result ->
            if (result.isSuccess) {
                widgetGateway.requestRefresh()
            }
        }
    }

    private fun resolveDisplayText(insights: HomeInsights): String = when {
        insights.envelopeCount == 0 -> WIDGET_SETUP_ENVELOPES
        insights.dailyAvailable.isOverBudget -> WIDGET_OVER_BUDGET
        insights.dailyAvailable.daysRemainingInWeek <= 0 -> WIDGET_NO_DAYS_LEFT
        insights.dailyAvailable.dailyAvailable != null ->
            formatPen(insights.dailyAvailable.dailyAvailable)
        else -> WIDGET_UNAVAILABLE
    }

    private fun formatPen(money: Money): String {
        val amount = money.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        return "S/ $amount"
    }

    private companion object {
        const val WIDGET_SETUP_ENVELOPES: String = "Configura sobres"
        const val WIDGET_OVER_BUDGET: String = "Presupuesto excedido"
        const val WIDGET_NO_DAYS_LEFT: String = "Semana terminada"
        const val WIDGET_UNAVAILABLE: String = "—"
    }
}
