package pe.kipu.core.domain.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** Abre el wizard de plan tras completar onboarding (persistido — cierra F14-01). */
    val pendingPlanWizard: Boolean = false,
    val antSpendingWeeklyLimitCents: Long? = null,
    val antSpendingAlertEnabled: Boolean = true,
    val antSpendingAlertPercent: Int = 80,
    val antSpendingTrackedCategories: Set<String> = emptySet(),
    /** Snapshot para widget de pantalla de inicio (sin PII). */
    val widgetDailyAvailableText: String? = null,
    val widgetIsOverBudget: Boolean = false,
    /** Momento del último cálculo persistido para no presentar el widget como actual. */
    val widgetDailyAvailableUpdatedAtMillis: Long? = null,
    val budgetCycle: BudgetCycle = BudgetCycle.WEEKLY,
)
