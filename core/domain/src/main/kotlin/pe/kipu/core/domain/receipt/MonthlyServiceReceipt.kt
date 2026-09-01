package pe.kipu.core.domain.receipt

import pe.kipu.core.domain.model.Money
import java.time.Instant

data class MonthlyServiceReceipt(
    val key: ServiceReceiptKey,
    val title: String,
    val configuredAmount: Money,
    val monthKey: String, // format "YYYY-MM", e.g. "2026-08"
    val isPaid: Boolean = false,
    val paidMovementId: String? = null,
    val paidAt: Instant? = null,
    val paidAmount: Money? = null,
)
