package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.MovementAuditEntity

@Dao
interface MovementAuditDao {

    @Query("SELECT * FROM movement_audit_logs ORDER BY timestampEpochMs DESC")
    fun observeAll(): Flow<List<MovementAuditEntity>>

    @Query("SELECT * FROM movement_audit_logs ORDER BY timestampEpochMs DESC")
    suspend fun getAll(): List<MovementAuditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MovementAuditEntity)

    @Query("DELETE FROM movement_audit_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM movement_audit_logs")
    suspend fun deleteAll()
}
