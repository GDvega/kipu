package pe.kipu.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.seed.DefaultCategorySeed
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.data.preferences.DataStoreUserPreferencesRepository
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class RoomUserDataWipeInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "kipu-wipe-test.db"
    private val prefsName = "kipu-wipe-test-prefs"

    private lateinit var database: KipuDatabase
    private lateinit var preferencesRepository: DataStoreUserPreferencesRepository
    private lateinit var wipeRepository: RoomUserDataWipeRepository

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        context.preferencesDataStoreFile(prefsName).delete()

        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            database.categoryDao().insertAll(DefaultCategorySeed.categories)
        }

        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(prefsName) },
        )
        preferencesRepository = DataStoreUserPreferencesRepository(
            dataStore = dataStore,
            applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        wipeRepository = RoomUserDataWipeRepository(database, preferencesRepository)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
        context.deleteDatabase(dbName)
    }

    @Test
    fun wipeClearsMovementsAndReseedsBaselineCategories() = runBlocking {
        val now = Instant.parse("2026-06-16T12:00:00Z")
        val movement = Movement(
            id = "movement-wipe-1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("25.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = now,
            createdAt = now,
        )
        database.movementDao().upsert(movement.toEntity())
        preferencesRepository.updatePreferences { it.copy(onboardingCompleted = true) }.getOrThrow()

        wipeRepository.wipeAllUserData().getOrThrow()

        assertEquals(0, database.movementDao().observeAll().first().size)
        assertTrue(database.categoryDao().observeAll().first().isNotEmpty())
        assertTrue(database.envelopeDao().observeAll().first().isEmpty())
        assertEquals(false, preferencesRepository.observePreferences().first().onboardingCompleted)
    }
}
