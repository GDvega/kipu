package pe.kipu.core.domain.plan

enum class IncomeProfile {
    FIXED,
    VARIABLE,
    APPROXIMATE,
}

enum class PayFrequency {
    MONTHLY,
    BIWEEKLY,
    WEEKLY,
}

fun IncomeProfile.title(): String = when (this) {
    IncomeProfile.FIXED -> "Tengo sueldo fijo"
    IncomeProfile.VARIABLE -> "Mis ingresos varían"
    IncomeProfile.APPROXIMATE -> "No sé exacto"
}

fun IncomeProfile.subtitle(): String = when (this) {
    IncomeProfile.FIXED -> "Recibo un monto fijo cada mes, quincena o semana"
    IncomeProfile.VARIABLE -> "Trabajo informal, freelance o ventas. Varía cada semana"
    IncomeProfile.APPROXIMATE -> "Pondré un aproximado y ajustaré después"
}

fun PayFrequency.label(): String = when (this) {
    PayFrequency.MONTHLY -> "Mensual"
    PayFrequency.BIWEEKLY -> "Quincenal"
    PayFrequency.WEEKLY -> "Semanal"
}
