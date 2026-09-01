package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_service_receipts",
    indices = [
        Index(value = ["monthKey"]),
        Index(value = ["monthKey", "serviceKeyIdentifier"], unique = true),
    ],
)
data class MonthlyServiceReceiptEntity(
    @PrimaryKey val id: String, // e.g. "2026-08_LIGHT"
    val monthKey: String, // "2026-08"
    val serviceKeyIdentifier: String, // "LIGHT", "WATER", "CUSTOM_GYM"
    val title: String,
    val configuredAmountCents: Long,
    val isPaid: Boolean,
    val paidMovementId: String? = null,
    val paidAtEpochMs: Long? = null,
)
