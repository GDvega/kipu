package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.MovementAuditDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.repository.MovementAuditRepository

@Singleton
class RoomMovementAuditRepository @Inject constructor(
    private val movementAuditDao: MovementAuditDao,
) : MovementAuditRepository {

    override fun observeAuditLogs(): Flow<List<MovementAuditEntry>> =
        movementAuditDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit> =
        runCatching {
            movementAuditDao.insert(entry.toEntity())
        }

    override suspend fun getAll(): List<MovementAuditEntry> =
        movementAuditDao.getAll().map { it.toDomain() }
}
