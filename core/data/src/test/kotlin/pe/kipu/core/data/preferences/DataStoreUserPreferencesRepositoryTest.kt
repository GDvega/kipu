package pe.kipu.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun updatePreferences_reportsDiskFailureAndRestoresInMemoryState() = runTest {
        val failingRepository = DataStoreUserPreferencesRepository(
            dataStore = FailingDataStore(),
            applicationScope = scope,
        )

        val result = failingRepository.updatePreferences {
            it.copy(onboardingCompleted = true)
        }

        assertTrue(result.isFailure)
        assertFalse(failingRepository.observePreferences().first().onboardingCompleted)
    }

    private class FailingDataStore : DataStore<Preferences> {
        override val data = flowOf(emptyPreferences())

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = throw IOException("disk full")
    }
}
