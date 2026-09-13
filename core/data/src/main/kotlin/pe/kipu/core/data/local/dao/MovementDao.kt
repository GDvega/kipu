package pe.kipu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
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

    @Transaction
    suspend fun upsert(entity: MovementEntity) {
        upsertMovement(entity)
        val validMonth = if (entity.type == "EXPENSE" && entity.status == "CONFIRMED") {
            YearMonth.from(Instant.ofEpochMilli(entity.recordedAtMillis).atZone(ZoneId.of("America/Lima"))).toString()
        } else null
        clearInvalidReceiptLinks(entity.id, validMonth)
        updateReceiptPaymentDate(entity.id, entity.recordedAtMillis)
    }

    @Upsert
    suspend fun upsertMovement(entity: MovementEntity)

    @Transaction
    suspend fun deleteById(id: String) {
        clearInvalidReceiptLinks(id, null)
        deleteMovementById(id)
    }

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteMovementById(id: String)

    @Query("""
        UPDATE monthly_service_receipts
        SET isPaid = 0, paidMovementId = NULL, paidAtEpochMs = NULL
        WHERE paidMovementId = :movementId AND (:validMonth IS NULL OR monthKey != :validMonth)
    """)
    suspend fun clearInvalidReceiptLinks(movementId: String, validMonth: String?)

    @Query("""
        UPDATE monthly_service_receipts SET paidAtEpochMs = :recordedAtMillis
        WHERE paidMovementId = :movementId AND isPaid = 1
    """)
    suspend fun updateReceiptPaymentDate(movementId: String, recordedAtMillis: Long)

    @Query("DELETE FROM movements")
    suspend fun deleteAll()
}
