package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface DuplicateDismissalRepository {
    fun observeDismissedPairKeys(): Flow<Set<String>>

    suspend fun dismiss(pairKey: String): Result<Unit>
}
