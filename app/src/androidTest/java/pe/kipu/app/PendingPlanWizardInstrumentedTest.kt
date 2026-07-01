package pe.kipu.app

import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.preferences.KipuPreferencesDataStore
import pe.kipu.core.data.preferences.UserPreferencesKeys

@RunWith(AndroidJUnit4::class)
class PendingPlanWizardInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun pendingPlanWizardFlagCanBeSetAndCleared() = runBlocking {
        val store = KipuPreferencesDataStore.get(context)
        store.edit { prefs ->
            prefs.clear()
            prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] = true
            prefs[UserPreferencesKeys.PENDING_PLAN_WIZARD] = true
        }

        val pendingBefore = store.data.first()[UserPreferencesKeys.PENDING_PLAN_WIZARD] ?: false
        assertTrue(pendingBefore)

        store.edit { prefs ->
            prefs[UserPreferencesKeys.PENDING_PLAN_WIZARD] = false
        }

        val pendingAfter = store.data.first()[UserPreferencesKeys.PENDING_PLAN_WIZARD] ?: false
        assertFalse(pendingAfter)
    }
}
