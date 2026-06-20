package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "envelopes",
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
data class EnvelopeEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Weekly limit in PEN centavos (scale 2). Spent amount is never stored. */
    val weeklyLimitCents: Long,
    val categoryId: String,
)
