package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reserve_events",
    indices = [
        Index(value = ["sourceMovementId"]),
        Index(value = ["reversesEventId"], unique = true),
        Index(value = ["occurredAtMillis"]),
    ],
)
data class ReserveEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    /** Kept without a foreign key so deleting a movement does not erase the reserve audit trail. */
    val sourceMovementId: String? = null,
    val reversesEventId: String? = null,
    val occurredAtMillis: Long,
    val createdAtMillis: Long,
)
