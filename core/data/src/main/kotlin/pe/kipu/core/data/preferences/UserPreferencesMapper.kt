package pe.kipu.core.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.model.UserPreferences

fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
    themeMode = parseThemeMode(this[UserPreferencesKeys.THEME_MODE]),
    notificationsEnabled = this[UserPreferencesKeys.NOTIFICATIONS_ENABLED] ?: false,
    onboardingCompleted = this[UserPreferencesKeys.ONBOARDING_COMPLETED] ?: false,
    pendingPlanWizard = this[UserPreferencesKeys.PENDING_PLAN_WIZARD] ?: false,
    antSpendingWeeklyLimitCents = this[UserPreferencesKeys.ANT_SPENDING_WEEKLY_LIMIT_CENTS],
    antSpendingAlertEnabled = this[UserPreferencesKeys.ANT_SPENDING_ALERT_ENABLED] ?: true,
    antSpendingAlertPercent = this[UserPreferencesKeys.ANT_SPENDING_ALERT_PERCENT] ?: 80,
    antSpendingTrackedCategories = parseCategorySet(this[UserPreferencesKeys.ANT_SPENDING_TRACKED_CATEGORIES]),
    widgetDailyAvailableText = this[UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_TEXT],
    widgetIsOverBudget = this[UserPreferencesKeys.WIDGET_IS_OVER_BUDGET] ?: false,
    widgetDailyAvailableUpdatedAtMillis = this[UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_UPDATED_AT_MILLIS],
    budgetCycle = parseBudgetCycle(this[UserPreferencesKeys.BUDGET_CYCLE]),
)

fun UserPreferences.toPreferences(): Preferences = mutablePreferencesOf(
    UserPreferencesKeys.THEME_MODE to themeMode.name,
    UserPreferencesKeys.NOTIFICATIONS_ENABLED to notificationsEnabled,
    UserPreferencesKeys.ONBOARDING_COMPLETED to onboardingCompleted,
    UserPreferencesKeys.PENDING_PLAN_WIZARD to pendingPlanWizard,
    UserPreferencesKeys.ANT_SPENDING_ALERT_ENABLED to antSpendingAlertEnabled,
    UserPreferencesKeys.ANT_SPENDING_ALERT_PERCENT to antSpendingAlertPercent,
    UserPreferencesKeys.ANT_SPENDING_TRACKED_CATEGORIES to antSpendingTrackedCategories.joinToString(CATEGORY_SEPARATOR),
).also { prefs ->
    antSpendingWeeklyLimitCents?.let { cents ->
        (prefs as MutablePreferences)[UserPreferencesKeys.ANT_SPENDING_WEEKLY_LIMIT_CENTS] = cents
    }
    val mutable = prefs as MutablePreferences
    val widgetText = widgetDailyAvailableText
    if (widgetText != null) {
        mutable[UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_TEXT] = widgetText
    } else {
        mutable.remove(UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_TEXT)
    }
    mutable[UserPreferencesKeys.WIDGET_IS_OVER_BUDGET] = widgetIsOverBudget
    val widgetUpdatedAtMillis = widgetDailyAvailableUpdatedAtMillis
    if (widgetUpdatedAtMillis != null) {
        mutable[UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_UPDATED_AT_MILLIS] = widgetUpdatedAtMillis
    } else {
        mutable.remove(UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_UPDATED_AT_MILLIS)
    }
    mutable[UserPreferencesKeys.BUDGET_CYCLE] = budgetCycle.name
}

internal fun MutablePreferences.applyUserPreferences(preferences: UserPreferences) {
    this[UserPreferencesKeys.THEME_MODE] = preferences.themeMode.name
    this[UserPreferencesKeys.NOTIFICATIONS_ENABLED] = preferences.notificationsEnabled
    this[UserPreferencesKeys.ONBOARDING_COMPLETED] = preferences.onboardingCompleted
    this[UserPreferencesKeys.PENDING_PLAN_WIZARD] = preferences.pendingPlanWizard
    this[UserPreferencesKeys.ANT_SPENDING_ALERT_ENABLED] = preferences.antSpendingAlertEnabled
    this[UserPreferencesKeys.ANT_SPENDING_ALERT_PERCENT] = preferences.antSpendingAlertPercent
    this[UserPreferencesKeys.ANT_SPENDING_TRACKED_CATEGORIES] =
        preferences.antSpendingTrackedCategories.joinToString(CATEGORY_SEPARATOR)
    preferences.antSpendingWeeklyLimitCents?.let {
        this[UserPreferencesKeys.ANT_SPENDING_WEEKLY_LIMIT_CENTS] = it
    } ?: remove(UserPreferencesKeys.ANT_SPENDING_WEEKLY_LIMIT_CENTS)
    preferences.widgetDailyAvailableText?.let {
        this[UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_TEXT] = it
    } ?: remove(UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_TEXT)
    this[UserPreferencesKeys.WIDGET_IS_OVER_BUDGET] = preferences.widgetIsOverBudget
    val widgetUpdatedAtMillis = preferences.widgetDailyAvailableUpdatedAtMillis
    if (widgetUpdatedAtMillis != null) {
        this[UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_UPDATED_AT_MILLIS] = widgetUpdatedAtMillis
    } else {
        remove(UserPreferencesKeys.WIDGET_DAILY_AVAILABLE_UPDATED_AT_MILLIS)
    }
    this[UserPreferencesKeys.BUDGET_CYCLE] = preferences.budgetCycle.name
}

fun parseThemeMode(raw: String?): ThemeMode {
    if (raw.isNullOrBlank()) return ThemeMode.SYSTEM
    return runCatching { ThemeMode.valueOf(raw) }
        .getOrNull()
        ?.takeIf { it in ThemeMode.entries }
        ?: ThemeMode.SYSTEM
}

fun parseBudgetCycle(raw: String?): BudgetCycle {
    if (raw.isNullOrBlank()) return BudgetCycle.WEEKLY
    return runCatching { BudgetCycle.valueOf(raw) }
        .getOrDefault(BudgetCycle.WEEKLY)
}

private fun parseCategorySet(raw: String?): Set<String> {
    if (raw.isNullOrBlank()) return emptySet()
    return raw.split(CATEGORY_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

private const val CATEGORY_SEPARATOR = "|"
