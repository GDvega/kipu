package pe.kipu.core.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.model.UserPreferences

class UserPreferencesMapperTest {

    @Test
    fun `empty preferences map to defaults`() {
        val result = emptyPreferences().toUserPreferences()

        assertEquals(ThemeMode.SYSTEM, result.themeMode)
        assertFalse(result.notificationsEnabled)
        assertFalse(result.onboardingCompleted)
    }

    @Test
    fun `stored values map to domain model`() {
        val preferences = mutablePreferencesOf(
            UserPreferencesKeys.THEME_MODE to ThemeMode.DARK.name,
            UserPreferencesKeys.NOTIFICATIONS_ENABLED to true,
            UserPreferencesKeys.ONBOARDING_COMPLETED to true,
        )

        val result = preferences.toUserPreferences()

        assertEquals(ThemeMode.DARK, result.themeMode)
        assertTrue(result.notificationsEnabled)
        assertTrue(result.onboardingCompleted)
    }

    @Test
    fun `invalid theme mode falls back to system`() {
        val preferences = mutablePreferencesOf(
            UserPreferencesKeys.THEME_MODE to "invalid_theme",
        )

        val result = preferences.toUserPreferences()

        assertEquals(ThemeMode.SYSTEM, result.themeMode)
    }

    @Test
    fun `domain model writes expected preference keys`() {
        val userPreferences = UserPreferences(
            themeMode = ThemeMode.LIGHT,
            notificationsEnabled = true,
            onboardingCompleted = false,
        )

        val preferences = userPreferences.toPreferences()

        assertEquals(ThemeMode.LIGHT.name, preferences[UserPreferencesKeys.THEME_MODE])
        assertEquals(true, preferences[UserPreferencesKeys.NOTIFICATIONS_ENABLED])
        assertEquals(false, preferences[UserPreferencesKeys.ONBOARDING_COMPLETED])
    }

    @Test
    fun `parseThemeMode accepts valid enum names`() {
        assertEquals(ThemeMode.DARK, parseThemeMode(ThemeMode.DARK.name))
        assertEquals(ThemeMode.LIGHT, parseThemeMode(ThemeMode.LIGHT.name))
        assertEquals(ThemeMode.SYSTEM, parseThemeMode(ThemeMode.SYSTEM.name))
    }

    @Test
    fun `parseThemeMode rejects unknown values`() {
        assertEquals(ThemeMode.SYSTEM, parseThemeMode(null))
        assertEquals(ThemeMode.SYSTEM, parseThemeMode(""))
        assertEquals(ThemeMode.SYSTEM, parseThemeMode("not_a_theme"))
    }

    @Test
    fun `preference keys are stable`() {
        assertEquals("theme_mode", UserPreferencesKeys.THEME_MODE.name)
        assertEquals("notifications_enabled", UserPreferencesKeys.NOTIFICATIONS_ENABLED.name)
        assertEquals("onboarding_completed", UserPreferencesKeys.ONBOARDING_COMPLETED.name)
        assertEquals("pending_plan_wizard", UserPreferencesKeys.PENDING_PLAN_WIZARD.name)
    }

    @Test
    fun `pending plan wizard and widget snapshot round trip`() {
        val userPreferences = UserPreferences(
            pendingPlanWizard = true,
            widgetDailyAvailableText = "S/ 40.00",
            widgetIsOverBudget = false,
        )

        val mapped = userPreferences.toPreferences().toUserPreferences()

        assertTrue(mapped.pendingPlanWizard)
        assertEquals("S/ 40.00", mapped.widgetDailyAvailableText)
        assertFalse(mapped.widgetIsOverBudget)
    }
}
