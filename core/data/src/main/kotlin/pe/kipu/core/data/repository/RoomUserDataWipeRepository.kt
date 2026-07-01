package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.room.withTransaction
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.KipuDatabaseSeeder
import pe.kipu.core.domain.repository.UserDataWipeRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository

@Singleton
class RoomUserDataWipeRepository @Inject constructor(
    private val database: KipuDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : UserDataWipeRepository {

    override suspend fun wipeAllUserData(): Result<Unit> = runCatching {
        userPreferencesRepository.clear().getOrThrow()
        database.withTransaction {
            database.movementDao().deleteAll()
            database.dismissedDuplicatePairDao().deleteAll()
            database.commitmentDao().deleteAll()
            database.financialPlanDao().deleteAll()
            database.envelopeDao().deleteAll()
            database.gatheringDao().deleteAll()
            database.gatheringExpenseDao().deleteAll()
            database.categoryDao().deleteAll()
        }
        withContext(Dispatchers.IO) {
            KipuDatabaseSeeder.seedBaseline(database.openHelper.writableDatabase)
        }
    }
}
