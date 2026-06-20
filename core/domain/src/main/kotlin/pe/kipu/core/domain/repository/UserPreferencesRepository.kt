package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.UserPreferences

interface UserPreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>

    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit>

    suspend fun clear(): Result<Unit>
}
