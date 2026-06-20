package pe.kipu.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun resolvesToDarkTheme_whenLightMode() {
        assertFalse(ThemeMode.LIGHT.resolvesToDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemeMode.LIGHT.resolvesToDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun resolvesToDarkTheme_whenDarkMode() {
        assertTrue(ThemeMode.DARK.resolvesToDarkTheme(systemInDarkTheme = false))
        assertTrue(ThemeMode.DARK.resolvesToDarkTheme(systemInDarkTheme = true))
    }

    @Test
    fun resolvesToDarkTheme_whenSystemMode() {
        assertTrue(ThemeMode.SYSTEM.resolvesToDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.resolvesToDarkTheme(systemInDarkTheme = false))
    }
}
