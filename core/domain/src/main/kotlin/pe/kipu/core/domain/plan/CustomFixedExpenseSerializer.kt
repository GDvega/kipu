package pe.kipu.core.domain.plan

object CustomFixedExpenseSerializer {

    private const val LINE_SEPARATOR = ";;"
    private const val FIELD_SEPARATOR = "|"

    fun serialize(items: List<PlanWizardLineItem>): String {
        if (items.isEmpty()) return ""
        return items
            .filter { it.label.isNotBlank() && it.amountText.isNotBlank() }
            .joinToString(LINE_SEPARATOR) { line ->
                val safeLabel = line.label.replace(FIELD_SEPARATOR, "/").replace(LINE_SEPARATOR, " ")
                val safeAmount = line.amountText.replace(FIELD_SEPARATOR, "").replace(LINE_SEPARATOR, "")
                "${line.id}$FIELD_SEPARATOR$safeLabel$FIELD_SEPARATOR$safeAmount"
            }
    }

    fun deserialize(raw: String?): List<PlanWizardLineItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(LINE_SEPARATOR).mapNotNull { part ->
            val fields = part.split(FIELD_SEPARATOR)
            if (fields.size >= 3) {
                PlanWizardLineItem(
                    id = fields[0],
                    label = fields[1],
                    amountText = fields[2],
                )
            } else {
                null
            }
        }
    }
}
