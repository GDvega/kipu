package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gatherings")
data class GatheringEntity(
    @PrimaryKey val id: String,
    val name: String,
    val participantCount: Int,
    /** Pipe-separated participant names. */
    val participantNames: String,
)
