package pe.kipu.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.Category

@RunWith(AndroidJUnit4::class)
class RoomRepositoryFirstEmissionInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: KipuDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun firstRepositoryEmissionContainsAlreadyPersistedRows() = runBlocking {
        val stored = Category(id = "category-stored", name = "Guardada")
        database.categoryDao().upsert(stored.toEntity())
        val repository = RoomCategoryRepository(database.categoryDao())

        val firstEmission = repository.observeCategories().first()

        assertEquals(listOf(stored), firstEmission)
    }
}
