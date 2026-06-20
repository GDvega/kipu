package pe.kipu.core.domain.plan

enum class GoalType {
    EMERGENCY,
    TRAVEL,
    PURCHASE,
    DEBT,
    DOLLARS,
}

fun GoalType.label(): String = when (this) {
    GoalType.EMERGENCY -> "Emergencia"
    GoalType.TRAVEL -> "Viaje"
    GoalType.PURCHASE -> "Compra"
    GoalType.DEBT -> "Pagar deuda"
    GoalType.DOLLARS -> "Dólares"
}

fun GoalType.defaultTitle(): String = when (this) {
    GoalType.EMERGENCY -> "Fondo de emergencia"
    GoalType.TRAVEL -> "Viaje"
    GoalType.PURCHASE -> "Compra"
    GoalType.DEBT -> "Pagar deuda"
    GoalType.DOLLARS -> "Ahorro en dólares"
}

object GoalTimeframeOptions {
    val MONTHS: List<Int> = listOf(3, 5, 8, 12)

    fun label(months: Int): String = when (months) {
        12 -> "1 año"
        else -> "$months meses"
    }
}
