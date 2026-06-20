package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gathering_expenses",
    foreignKeys = [
        ForeignKey(
            entity = GatheringEntity::class,
            parentColumns = ["id"],
            childColumns = ["gatheringId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MovementEntity::class,
            parentColumns = ["id"],
            childColumns = ["movementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("gatheringId"),
        Index(value = ["movementId"], unique = true),
    ],
)
data class GatheringExpenseEntity(
    @PrimaryKey val id: String,
    val gatheringId: String,
    val amountCents: Long,
    val paidByParticipant: String,
    val description: String?,
    val movementId: String?,
    val recordedAtMillis: Long,
)
