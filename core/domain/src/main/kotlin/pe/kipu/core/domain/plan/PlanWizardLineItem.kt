package pe.kipu.core.domain.plan

/**
 * Editable name + amount row in the plan wizard (income extras or custom fixed expenses).
 */
data class PlanWizardLineItem(
    val id: String,
    val label: String,
    val amountText: String,
    val categoryId: String? = null,
)
