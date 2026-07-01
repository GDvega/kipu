package pe.kipu.core.designsystem.component

/** Stable test tags for Compose UI tests. */
object KipuTestTags {
    const val REGISTER_FAB = "kipu_register_fab"
    const val DAILY_AVAILABLE_HERO = "kipu_daily_available_hero"
    const val DIALOG_CONFIRM = "kipu_dialog_confirm"
    const val DIALOG_DISMISS = "kipu_dialog_dismiss"

    fun bottomBarTab(route: String): String = "kipu_bottom_bar_$route"
}
