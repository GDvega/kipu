package pe.kipu.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.UserPreferencesRepository

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override fun observePreferences(): Flow<UserPreferences> =
        dataStore.data.map { preferences -> preferences.toUserPreferences() }

    override suspend fun updatePreferences(
        transform: (UserPreferences) -> UserPreferences,
    ): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toUserPreferences())
            preferences.applyUserPreferences(updated)
        }
    }

    override suspend fun clear(): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
