package pe.kipu.app

import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.preferences.KipuPreferencesDataStore
import pe.kipu.core.data.preferences.UserPreferencesKeys

@RunWith(AndroidJUnit4::class)
class PlanWizardLoadInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun onboardingCompletedWithPendingPlanWizardIsPersisted() = runBlocking {
        val store = KipuPreferencesDataStore.get(context)
        store.edit { prefs ->
            prefs.clear()
            prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] = true
            prefs[UserPreferencesKeys.PENDING_PLAN_WIZARD] = true
        }

        val prefs = store.data.first()
        assertTrue(prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] ?: false)
        assertTrue(prefs[UserPreferencesKeys.PENDING_PLAN_WIZARD] ?: false)
    }
}
