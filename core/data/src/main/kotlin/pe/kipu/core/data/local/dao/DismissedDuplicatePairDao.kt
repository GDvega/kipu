package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.DismissedDuplicatePairEntity

@Dao
interface DismissedDuplicatePairDao {
    @Query("SELECT pairKey FROM dismissed_duplicate_pairs")
    fun observePairKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DismissedDuplicatePairEntity)

    @Query("DELETE FROM dismissed_duplicate_pairs")
    suspend fun deleteAll()
}
