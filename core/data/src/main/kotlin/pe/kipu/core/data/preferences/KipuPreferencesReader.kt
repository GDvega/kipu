package pe.kipu.core.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.UserPreferences

/** Reads user preferences from the shared DataStore file (widget / background). */
suspend fun Context.readKipuUserPreferences(): UserPreferences =
    KipuPreferencesDataStore.get(this).data.first().toUserPreferences()
