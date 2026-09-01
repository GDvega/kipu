package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.MonthlyServiceReceiptEntity

@Dao
interface MonthlyServiceReceiptDao {
    @Query("SELECT * FROM monthly_service_receipts WHERE monthKey = :monthKey ORDER BY serviceKeyIdentifier ASC")
    fun observeForMonth(monthKey: String): Flow<List<MonthlyServiceReceiptEntity>>

    @Query("SELECT paidMovementId FROM monthly_service_receipts WHERE isPaid = 1 AND paidMovementId IS NOT NULL")
    fun observeAllPaidMovementIds(): Flow<List<String>>

    @Query("SELECT * FROM monthly_service_receipts WHERE monthKey = :monthKey AND serviceKeyIdentifier = :serviceKeyIdentifier LIMIT 1")
    suspend fun getByKey(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceiptEntity?

    @Query("SELECT * FROM monthly_service_receipts ORDER BY monthKey DESC, serviceKeyIdentifier ASC")
    suspend fun getAll(): List<MonthlyServiceReceiptEntity>

    @Upsert
    suspend fun upsert(entity: MonthlyServiceReceiptEntity)

    @Query("DELETE FROM monthly_service_receipts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM monthly_service_receipts")
    suspend fun deleteAll()
}
