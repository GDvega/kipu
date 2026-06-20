package pe.kipu.app

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.preferences.readKipuUserPreferences
import pe.kipu.core.data.preferences.toPreferences
import pe.kipu.core.domain.model.UserPreferences

private val Context.kipuSeedDataStore by preferencesDataStore(name = "kipu_preferences")

@RunWith(AndroidJUnit4::class)
class PendingPlanWizardInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedPendingWizardAndRecreate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            context.kipuSeedDataStore.edit { prefs ->
                prefs.clear()
                prefs.putAll(
                    UserPreferences(
                        onboardingCompleted = true,
                        pendingPlanWizard = true,
                    ).toPreferences(),
                )
            }
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun opensPlanWizardWhenPendingFlagIsPersisted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching {
                composeRule.onNodeWithText("¿Cuánto dinero recibes?").assertExists()
                true
            }.getOrDefault(false)
        }

        runBlocking {
            val preferences = context.readKipuUserPreferences()
            check(!preferences.pendingPlanWizard) {
                "Wizard navigation must clear pendingPlanWizard after opening"
            }
        }
    }
}
