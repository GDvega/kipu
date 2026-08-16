package pe.kipu.core.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object UserPreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val PENDING_PLAN_WIZARD = booleanPreferencesKey("pending_plan_wizard")
    val WIDGET_DAILY_AVAILABLE_TEXT = stringPreferencesKey("widget_daily_available_text")
    val WIDGET_IS_OVER_BUDGET = booleanPreferencesKey("widget_is_over_budget")
    val WIDGET_DAILY_AVAILABLE_UPDATED_AT_MILLIS = longPreferencesKey("widget_daily_available_updated_at_millis")
    val ANT_SPENDING_WEEKLY_LIMIT_CENTS = longPreferencesKey("ant_spending_weekly_limit_cents")
    val ANT_SPENDING_ALERT_ENABLED = booleanPreferencesKey("ant_spending_alert_enabled")
    val ANT_SPENDING_ALERT_PERCENT = intPreferencesKey("ant_spending_alert_percent")
    val ANT_SPENDING_TRACKED_CATEGORIES = stringPreferencesKey("ant_spending_tracked_categories")
    val BUDGET_CYCLE = stringPreferencesKey("budget_cycle")
}
