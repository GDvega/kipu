package pe.kipu.core.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pe.kipu.core.domain.repository.DuplicateDismissalRepository

class InMemoryDuplicateDismissalRepository : DuplicateDismissalRepository {

    private val dismissedPairKeys = MutableStateFlow<Set<String>>(emptySet())

    override fun observeDismissedPairKeys(): Flow<Set<String>> =
        dismissedPairKeys.map { keys -> keys.toSet() }

    override suspend fun dismiss(pairKey: String): Result<Unit> {
        dismissedPairKeys.value = dismissedPairKeys.value + pairKey
        return Result.success(Unit)
    }
}
