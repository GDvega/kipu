package pe.kipu.core.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.domain.repository.LocalTransactionRunner

@Singleton
class RoomLocalTransactionRunner @Inject constructor(
    private val database: KipuDatabase,
) : LocalTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): Result<T> = try {
        Result.success(database.withTransaction { block() })
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}
