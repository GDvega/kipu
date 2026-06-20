package pe.kipu.core.domain.widget

/**
 * Notifies the home-screen widget to reload its snapshot from preferences.
 * Implementation lives in the app module (Glance).
 */
interface DailyAvailableWidgetGateway {
    suspend fun requestRefresh()
}
