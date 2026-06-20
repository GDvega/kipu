package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
data class MovementEntity(
    @PrimaryKey val id: String,
    val type: String,
    /** PEN amount stored as centavos (scale 2). */
    val amountCents: Long,
    val categoryId: String,
    val channel: String,
    val source: String,
    val status: String,
    val description: String?,
    val counterpartyName: String?,
    val operationNumber: String?,
    val recordedAtMillis: Long,
    val createdAtMillis: Long,
)
