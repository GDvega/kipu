package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.MovementEntity

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements ORDER BY recordedAtMillis DESC")
    fun observeAll(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MovementEntity?

    @Query(
        """
        SELECT * FROM movements
        WHERE counterpartyName IS NOT NULL
        AND LOWER(counterpartyName) = LOWER(:counterpartyName)
        ORDER BY recordedAtMillis DESC
        """,
    )
    suspend fun findByCounterpartyName(counterpartyName: String): List<MovementEntity>

    @Upsert
    suspend fun upsert(entity: MovementEntity)

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM movements")
    suspend fun deleteAll()
}
