package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.EnvelopeEntity

@Dao
interface EnvelopeDao {
    @Query("SELECT * FROM envelopes ORDER BY name ASC")
    fun observeAll(): Flow<List<EnvelopeEntity>>

    @Query("SELECT * FROM envelopes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EnvelopeEntity?

    @Upsert
    suspend fun upsert(entity: EnvelopeEntity)

    @Query("DELETE FROM envelopes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM envelopes")
    suspend fun deleteAll()
}
