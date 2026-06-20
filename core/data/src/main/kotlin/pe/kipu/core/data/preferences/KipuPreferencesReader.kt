package pe.kipu.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.UserPreferences

object KipuPreferencesStore {
    const val FILE_NAME: String = "kipu_preferences"
}

private val Context.kipuUserPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = KipuPreferencesStore.FILE_NAME,
)

/** Reads user preferences from the shared DataStore file (widget / background). */
suspend fun Context.readKipuUserPreferences(): UserPreferences =
    kipuUserPreferencesDataStore.data.first().toUserPreferences()
