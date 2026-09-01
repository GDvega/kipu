package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.MovementAuditEntry

interface MovementAuditRepository {
    fun observeAuditLogs(): Flow<List<MovementAuditEntry>>
    suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit>
    suspend fun getAll(): List<MovementAuditEntry>
}
