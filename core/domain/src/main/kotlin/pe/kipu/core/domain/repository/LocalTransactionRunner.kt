package pe.kipu.core.domain.repository

import kotlinx.coroutines.CancellationException

interface LocalTransactionRunner {
    suspend fun <T> run(block: suspend () -> T): Result<T>
}

object DirectLocalTransactionRunner : LocalTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}
