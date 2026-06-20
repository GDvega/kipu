package pe.kipu.core.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.data.local.seed.DefaultCategorySeed
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class MovementDaoInstrumentedTest {

    private lateinit var database: KipuDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DefaultCategorySeed.categories.forEach { category ->
            runBlocking { database.categoryDao().insertAll(listOf(category)) }
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveMovement() = runBlocking {
        val now = Instant.parse("2026-06-16T12:00:00Z")
        val movement = Movement(
            id = "movement-test-1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("20.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            description = "Prueba instrumentada",
            counterpartyName = "Tienda",
            recordedAt = now,
            createdAt = now,
        )

        database.movementDao().upsert(movement.toEntity())

        val stored = database.movementDao().observeAll().first()

        assertEquals(1, stored.size)
        assertEquals("movement-test-1", stored.first().id)
        assertEquals(2_000L, stored.first().amountCents)
    }

    @Test
    fun findByCounterpartyNameIsCaseInsensitive() = runBlocking {
        val now = Instant.parse("2026-06-16T12:00:00Z")
        val movement = Movement(
            id = "movement-test-2",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("15.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.PLIN,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "Luis Lopez",
            recordedAt = now,
            createdAt = now,
        )

        database.movementDao().upsert(movement.toEntity())

        val matches = database.movementDao().findByCounterpartyName("luis lopez")

        assertEquals(1, matches.size)
        assertEquals("movement-test-2", matches.first().id)
    }
}
