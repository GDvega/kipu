package pe.kipu.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import pe.kipu.core.data.preferences.readKipuUserPreferences
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.domain.time.CycleRangeCalculator

class DailyAvailableGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = context.readKipuUserPreferences()
        val amountText = preferences.widgetDailyAvailableText ?: "—"
        val isOverBudget = preferences.widgetIsOverBudget
        val updatedAtText = formatDailyAvailableWidgetUpdatedAt(
            updatedAtMillis = preferences.widgetDailyAvailableUpdatedAtMillis,
        )

        provideContent {
            GlanceTheme {
                DailyAvailableWidgetContent(
                    amountText = amountText,
                    isOverBudget = isOverBudget,
                    updatedAtText = updatedAtText,
                )
            }
        }
    }
}

@Composable
private fun DailyAvailableWidgetContent(
    amountText: String,
    isOverBudget: Boolean,
    updatedAtText: String,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF0A0A0F)))
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = "Disponible hoy",
            style = TextStyle(
                color = ColorProvider(Color(0xFF9CA3AF)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = amountText,
            style = TextStyle(
                color = if (isOverBudget) {
                    ColorProvider(KipuRed)
                } else {
                    ColorProvider(KipuPrimary)
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
        Text(
            text = updatedAtText,
            style = TextStyle(
                color = ColorProvider(Color(0xFF9CA3AF)),
                fontSize = 11.sp,
            ),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
    }
}

internal fun formatDailyAvailableWidgetUpdatedAt(
    updatedAtMillis: Long?,
    zoneId: ZoneId = CycleRangeCalculator.PERU_ZONE,
): String = updatedAtMillis?.let { instantMillis ->
    "Actualizado ${WIDGET_UPDATED_AT_FORMATTER.format(Instant.ofEpochMilli(instantMillis).atZone(zoneId))}"
} ?: "Sin actualizar"

private val WIDGET_UPDATED_AT_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")

class DailyAvailableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyAvailableGlanceWidget()
}
