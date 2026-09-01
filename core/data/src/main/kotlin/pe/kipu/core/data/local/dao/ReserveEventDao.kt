package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.ReserveEventEntity

@Dao
interface ReserveEventDao {
    @Query("SELECT * FROM reserve_events ORDER BY occurredAtMillis ASC, createdAtMillis ASC, id ASC")
    fun observeAll(): Flow<List<ReserveEventEntity>>

    @Query("SELECT * FROM reserve_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReserveEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ReserveEventEntity)

    @Query(
        """
        SELECT COUNT(*) FROM reserve_events AS use_event
        WHERE use_event.sourceMovementId = :movementId
          AND use_event.type = 'USE'
          AND NOT EXISTS (
              SELECT 1 FROM reserve_events AS reversal
              WHERE reversal.type = 'REVERSAL'
                AND reversal.reversesEventId = use_event.id
          )
        """,
    )
    suspend fun countActiveUses(movementId: String): Int

    @Transaction
    suspend fun insertValidated(entity: ReserveEventEntity) {
        if (entity.type == "USE") {
            val movementId = requireNotNull(entity.sourceMovementId)
            require(countActiveUses(movementId) == 0) {
                "Movement already has an active reserve use"
            }
        }
        insert(entity)
    }

    @Query("DELETE FROM reserve_events")
    suspend fun deleteAll()
}
