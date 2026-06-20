package pe.kipu.core.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

fun ThemeMode.resolvesToDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemInDarkTheme
}
