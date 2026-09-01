package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry

interface MonthlyServiceReceiptRepository {
    fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>>
    fun observeAllPaidMovementIds(): Flow<Set<String>>
    suspend fun saveReceipt(receipt: MonthlyServiceReceipt)
    suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt?
    suspend fun getAll(): List<MonthlyServiceReceipt> = emptyList()
    suspend fun markPaid(
        receipt: MonthlyServiceReceipt,
        movement: Movement,
        auditEntry: MovementAuditEntry,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Atomic receipt payment is not supported"))

    suspend fun unmarkPaid(
        receipt: MonthlyServiceReceipt,
        movementId: String?,
        auditEntry: MovementAuditEntry?,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Atomic receipt payment is not supported"))
}
