package pe.kipu.core.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository

@Singleton
class RoomMonthlyServiceReceiptRepository @Inject constructor(
    private val database: KipuDatabase,
) : MonthlyServiceReceiptRepository {

    private val monthlyServiceReceiptDao get() = database.monthlyServiceReceiptDao()

    override fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>> =
        monthlyServiceReceiptDao.observeForMonth(monthKey)
            .map { list -> list.map { it.toDomain() } }

    override fun observeAllPaidMovementIds(): Flow<Set<String>> =
        monthlyServiceReceiptDao.observeAllPaidMovementIds()
            .map { it.toSet() }

    override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) {
        monthlyServiceReceiptDao.upsert(receipt.toEntity())
    }

    override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt? =
        monthlyServiceReceiptDao.getByKey(monthKey, serviceKeyIdentifier)?.toDomain()

    override suspend fun getAll(): List<MonthlyServiceReceipt> =
        monthlyServiceReceiptDao.getAll().map { it.toDomain() }

    override suspend fun markPaid(
        receipt: MonthlyServiceReceipt,
        movement: Movement,
        auditEntry: MovementAuditEntry,
    ): Result<Unit> {
        if (movement.validate() is DomainResult.Err) {
            return Result.failure(IllegalArgumentException("Invalid movement"))
        }
        return try {
            database.withTransaction {
                database.movementDao().upsert(movement.toEntity())
                monthlyServiceReceiptDao.upsert(receipt.toEntity())
                database.movementAuditDao().insert(auditEntry.toEntity())
            }
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun unmarkPaid(
        receipt: MonthlyServiceReceipt,
        movementId: String?,
        auditEntry: MovementAuditEntry?,
    ): Result<Unit> = try {
        database.withTransaction {
            movementId?.let { database.movementDao().deleteById(it) }
            monthlyServiceReceiptDao.upsert(receipt.toEntity())
            auditEntry?.let { database.movementAuditDao().insert(it.toEntity()) }
        }
        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}
