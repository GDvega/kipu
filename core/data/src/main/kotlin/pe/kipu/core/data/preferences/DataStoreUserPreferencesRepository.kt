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
import kotlinx.coroutines.withTimeout
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
        val updated = transform(preferencesState.value)
        preferencesState.value = updated
        pendingDiskWrite = updated
        return persistToDisk(updated)
    }

    override suspend fun clear(): Result<Unit> = runCatching {
        pendingDiskWrite = UserPreferences()
        dataStore.edit { preferences -> preferences.clear() }
        preferencesState.value = UserPreferences()
        pendingDiskWrite = null
    }

    private suspend fun persistToDisk(preferences: UserPreferences): Result<Unit> {
        val diskResult = runCatching {
            withTimeout(DISK_WRITE_TIMEOUT_MS) {
                dataStore.edit { prefs -> prefs.applyUserPreferences(preferences) }
            }
        }
        if (diskResult.isFailure) {
            applicationScope.launch {
                runCatching {
                    dataStore.edit { prefs -> prefs.applyUserPreferences(preferences) }
                }
            }
        }
        return Result.success(Unit)
    }

    private companion object {
        const val DISK_WRITE_TIMEOUT_MS: Long = 5_000L
    }
}
