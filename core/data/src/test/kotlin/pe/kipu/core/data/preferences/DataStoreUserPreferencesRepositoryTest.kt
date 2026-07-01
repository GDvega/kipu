package pe.kipu.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataStoreUserPreferencesRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreUserPreferencesRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("kipu-prefs-test").toFile()
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempDir, "test_prefs.preferences_pb") },
        )
        repository = DataStoreUserPreferencesRepository(
            dataStore = dataStore,
            applicationScope = scope,
        )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun observePreferences_emitsDefaultWithoutBlocking() = runTest {
        val preferences = repository.observePreferences().first()

        assertFalse(preferences.onboardingCompleted)
        assertFalse(preferences.pendingPlanWizard)
    }

    @Test
    fun updatePreferences_isReadableImmediatelyBeforeDiskWriteCompletes() = runTest {
        val preferences = repository.updatePreferences {
            it.copy(onboardingCompleted = true)
        }

        assertTrue(preferences.isSuccess)
        assertTrue(repository.observePreferences().first().onboardingCompleted)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun observePreferences_reflectsPersistedValues() = runTest {
        repository.updatePreferences {
            it.copy(onboardingCompleted = true, pendingPlanWizard = true)
        }
        advanceUntilIdle()

        val preferences = repository.observePreferences().first()

        assertTrue(preferences.onboardingCompleted)
        assertTrue(preferences.pendingPlanWizard)
    }
}
