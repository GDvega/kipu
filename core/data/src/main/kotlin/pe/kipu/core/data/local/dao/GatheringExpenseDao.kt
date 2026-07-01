package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.data.local.entity.GatheringExpenseEntity

@Dao
interface GatheringExpenseDao {

    @Query("SELECT * FROM gathering_expenses ORDER BY recordedAtMillis DESC")
    fun observeAll(): Flow<List<GatheringExpenseEntity>>

    @Query(
        """
        SELECT gatheringId, SUM(amountCents) AS totalCents
        FROM gathering_expenses
        GROUP BY gatheringId
        """,
    )
    fun observeTotalsByGathering(): Flow<List<GatheringExpenseTotalRow>>

    @Query("SELECT movementId FROM gathering_expenses WHERE movementId IS NOT NULL")
    fun observeLinkedMovementIds(): Flow<List<String>>

    @Query(
        """
        SELECT ge.movementId FROM gathering_expenses ge
        INNER JOIN gatherings g ON ge.gatheringId = g.id
        WHERE ge.movementId IS NOT NULL AND g.isSettled = 0
        """,
    )
    fun observeActiveGatheringLinkedMovementIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) > 0 FROM gathering_expenses WHERE movementId = :movementId LIMIT 1")
    suspend fun isMovementLinked(movementId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: GatheringExpenseEntity)

    @Query("DELETE FROM gathering_expenses")
    suspend fun deleteAll()
}

data class GatheringExpenseTotalRow(
    val gatheringId: String,
    val totalCents: Long,
)
