package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.GatheringEntity

@Dao
interface GatheringDao {
    @Query("SELECT * FROM gatherings ORDER BY name ASC")
    fun observeAll(): Flow<List<GatheringEntity>>

    @Query("SELECT * FROM gatherings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GatheringEntity?

    @Upsert
    suspend fun upsert(entity: GatheringEntity)

    @Query("DELETE FROM gatherings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM gatherings")
    suspend fun deleteAll()
}
