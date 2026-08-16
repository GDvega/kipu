package pe.kipu.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pe.kipu.core.data.di.ApplicationScope
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.UserPreferencesRepository

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : UserPreferencesRepository {

    private val preferencesState = MutableStateFlow(UserPreferences())
    @Volatile
    private var pendingDiskWrite: UserPreferences? = null

    init {
        applicationScope.launch {
            dataStore.data
                .map { preferences -> preferences.toUserPreferences() }
                .collect { loaded ->
                    val pending = pendingDiskWrite
                    if (pending == null || loaded == pending) {
                        preferencesState.value = loaded
                        if (loaded == pending) {
                            pendingDiskWrite = null
                        }
                    }
                }
        }
    }

    override fun observePreferences(): Flow<UserPreferences> = preferencesState

    override suspend fun updatePreferences(
        transform: (UserPreferences) -> UserPreferences,
    ): Result<Unit> {
        val previous = preferencesState.value
        val updated = transform(preferencesState.value)
        preferencesState.value = updated
        pendingDiskWrite = updated
        val diskResult = persistToDisk(updated)
        if (diskResult.isFailure) {
            pendingDiskWrite = null
            preferencesState.value = previous
        }
        return diskResult
    }

    override suspend fun clear(): Result<Unit> {
        val previous = preferencesState.value
        pendingDiskWrite = UserPreferences()
        val diskResult = runCatching {
            dataStore.edit { preferences -> preferences.clear() }
            Unit
        }
        if (diskResult.isSuccess) {
            preferencesState.value = UserPreferences()
        } else {
            preferencesState.value = previous
        }
        pendingDiskWrite = null
        return diskResult
    }

    private suspend fun persistToDisk(preferences: UserPreferences): Result<Unit> {
        return runCatching {
            dataStore.edit { prefs -> prefs.applyUserPreferences(preferences) }
            Unit
        }
    }
}
