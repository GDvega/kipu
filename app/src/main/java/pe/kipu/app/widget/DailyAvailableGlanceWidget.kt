package pe.kipu.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import pe.kipu.core.data.preferences.readKipuUserPreferences

class DailyAvailableGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = context.readKipuUserPreferences()
        val amountText = preferences.widgetDailyAvailableText ?: "—"
        val isOverBudget = preferences.widgetIsOverBudget

        provideContent {
            GlanceTheme {
                DailyAvailableWidgetContent(
                    amountText = amountText,
                    isOverBudget = isOverBudget,
                )
            }
        }
    }
}

@Composable
private fun DailyAvailableWidgetContent(
    amountText: String,
    isOverBudget: Boolean,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = "Disponible hoy",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = amountText,
            style = TextStyle(
                color = if (isOverBudget) {
                    ColorProvider(android.graphics.Color.parseColor("#C62828"))
                } else {
                    GlanceTheme.colors.primary
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
    }
}

class DailyAvailableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyAvailableGlanceWidget()
}
