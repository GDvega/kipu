package pe.kipu.core.domain.model

/**
 * Derived alert for several small expenses in a short window.
 * [messageKey] is a domain key translated in presentation.
 */
data class AntSpendingAlert(
    val severity: AlertSeverity,
    val transactionCount: Int,
    val totalAmount: Money,
    val windowHours: Int,
    val categoryId: EntityId?,
    val messageKey: String,
)
