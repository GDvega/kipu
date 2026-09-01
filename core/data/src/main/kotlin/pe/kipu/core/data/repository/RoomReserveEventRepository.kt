package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.ReserveEventDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.repository.ReserveEventRepository

@Singleton
class RoomReserveEventRepository @Inject constructor(
    private val reserveEventDao: ReserveEventDao,
) : ReserveEventRepository {
    override fun observeAll(): Flow<List<ReserveEvent>> =
        reserveEventDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): ReserveEvent? = reserveEventDao.getById(id)?.toDomain()

    override suspend fun record(event: ReserveEvent): Result<Unit> {
        if (event.validate() is DomainResult.Err) {
            return Result.failure(IllegalArgumentException("Invalid reserve event"))
        }
        return try {
            reserveEventDao.insertValidated(event.toEntity())
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
