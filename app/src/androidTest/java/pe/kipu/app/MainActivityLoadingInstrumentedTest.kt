package pe.kipu.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import pe.kipu.core.data.preferences.KipuPreferencesDataStore
import pe.kipu.core.data.preferences.UserPreferencesKeys

@RunWith(AndroidJUnit4::class)
class MainActivityLoadingInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val seedOnboardingCompleted = object : ExternalResource() {
        override fun before() {
            runBlocking {
                KipuPreferencesDataStore.get(context).edit { prefs ->
                    prefs[UserPreferencesKeys.ONBOARDING_COMPLETED] = true
                    prefs[UserPreferencesKeys.PENDING_PLAN_WIZARD] = false
                }
            }
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(seedOnboardingCompleted)
        .around(composeRule)

    @Test
    fun mainActivityExitsLoadingSpinnerWithinTimeout() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("Inicio").assertExists()
                true
            }.getOrDefault(false)
        }
    }
}
