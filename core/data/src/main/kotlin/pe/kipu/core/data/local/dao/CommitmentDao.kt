package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.CommitmentEntity

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments ORDER BY title ASC")
    fun observeAll(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CommitmentEntity?

    @Query("SELECT id FROM commitments WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: Set<String>): List<String>

    @Upsert
    suspend fun upsert(entity: CommitmentEntity)

    @Query("UPDATE commitments SET isSettled = 1 WHERE id IN (:ids)")
    suspend fun settleByIds(ids: Set<String>)

    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM commitments")
    suspend fun deleteAll()
}
