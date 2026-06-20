package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.FinancialPlanEntity

@Dao
interface FinancialPlanDao {
    @Query("SELECT * FROM financial_plans ORDER BY id ASC")
    fun observeAll(): Flow<List<FinancialPlanEntity>>

    @Query("SELECT * FROM financial_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FinancialPlanEntity?

    @Upsert
    suspend fun upsert(entity: FinancialPlanEntity)

    @Query("DELETE FROM financial_plans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM financial_plans")
    suspend fun deleteAll()
}
