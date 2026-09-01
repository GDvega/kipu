package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movement_audit_logs",
    indices = [
        Index(value = ["movementId"]),
        Index(value = ["timestampEpochMs"]),
        Index(value = ["action"]),
    ],
)
data class MovementAuditEntity(
    @PrimaryKey
    val id: String,
    val movementId: String,
    val action: String, // "CREATED", "UPDATED", "DELETED"
    val movementType: String, // "EXPENSE", "INCOME"
    val amountCents: Long,
    val categoryId: String,
    val categoryName: String? = null,
    val channel: String, // "YAPE", "PLIN", "CASH", etc.
    val description: String? = null,
    val counterpartyName: String? = null,
    val details: String? = null,
    val timestampEpochMs: Long,
)
