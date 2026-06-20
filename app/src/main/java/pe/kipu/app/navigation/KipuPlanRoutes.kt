package pe.kipu.app.navigation

object KipuPlanRoutes {
    const val WIZARD = "plan/{startStep}"
    const val START_STEP_ARG = "startStep"

    const val STEP_INCOME = "income"
    const val STEP_EXPENSES = "expenses"
    const val STEP_ENVELOPES = "envelopes"
    const val STEP_SUMMARY = "summary"

    fun wizard(startStep: String = STEP_INCOME): String = "plan/$startStep"

    const val MOVEMENTS_BY_CATEGORY = "movements/category/{categoryId}"
    const val CATEGORY_ID_ARG = "categoryId"

    fun movementsByCategory(categoryId: String): String = "movements/category/$categoryId"
}
