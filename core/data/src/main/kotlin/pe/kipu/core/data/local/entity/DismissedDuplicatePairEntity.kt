package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dismissed_duplicate_pairs")
data class DismissedDuplicatePairEntity(
    @PrimaryKey val pairKey: String,
    val dismissedAtMillis: Long,
)
