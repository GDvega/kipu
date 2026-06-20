package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.DismissedDuplicatePairDao
import pe.kipu.core.data.local.entity.DismissedDuplicatePairEntity
import pe.kipu.core.domain.repository.DuplicateDismissalRepository

@Singleton
class RoomDuplicateDismissalRepository @Inject constructor(
    private val dismissedDuplicatePairDao: DismissedDuplicatePairDao,
) : DuplicateDismissalRepository {

    override fun observeDismissedPairKeys(): Flow<Set<String>> =
        dismissedDuplicatePairDao.observePairKeys().map { keys -> keys.toSet() }

    override suspend fun dismiss(pairKey: String): Result<Unit> =
        runCatching {
            dismissedDuplicatePairDao.insert(
                DismissedDuplicatePairEntity(
                    pairKey = pairKey,
                    dismissedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
}
