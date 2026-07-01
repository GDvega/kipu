package pe.kipu.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

/**
 * Single DataStore instance for `kipu_preferences`.
 * Hilt and the home-screen widget must share this to avoid concurrent file access.
 */
object KipuPreferencesDataStore {

    const val FILE_NAME: String = "kipu_preferences"

    @Volatile
    private var store: DataStore<Preferences>? = null

    fun get(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext
        return store ?: synchronized(this) {
            store ?: PreferenceDataStoreFactory.create(
                produceFile = {
                    appContext.preferencesDataStoreFile(FILE_NAME)
                },
            ).also { store = it }
        }
    }
}
