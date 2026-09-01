package pe.kipu.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError

@RunWith(AndroidJUnit4::class)
class RoomReserveEventRepositoryInstrumentedTest {
    private lateinit var database: KipuDatabase
    private lateinit var repository: RoomReserveEventRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomReserveEventRepository(database.reserveEventDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun recordPersistsUseAndRejectsASecondUseForTheSameMovement() = runBlocking {
        val now = Instant.parse("2026-08-26T15:00:00Z")
        val first = ReserveEvent(
            id = "reserve-use-1",
            type = ReserveEventType.USE,
            amount = Money.of(BigDecimal("100.00")).getOrError(),
            sourceMovementId = "movement-1",
            occurredAt = now,
            createdAt = now,
        )
        val duplicate = first.copy(id = "reserve-use-2")

        assertTrue(repository.record(first).isSuccess)
        assertTrue(repository.record(duplicate).isFailure)
        assertEquals(first, repository.getById(first.id))
        assertEquals(listOf(first), repository.observeAll().first())
    }

    @Test
    fun recordAllowsAReplacementUseOnlyAfterThePreviousUseIsReversed() = runBlocking {
        val now = Instant.parse("2026-08-26T15:00:00Z")
        val first = reserveUse("reserve-use-1", "movement-1", "100.00", now)
        val reversal = ReserveEvent(
            id = "reserve-reversal-1",
            type = ReserveEventType.REVERSAL,
            amount = first.amount,
            reversesEventId = first.id,
            occurredAt = now,
            createdAt = now,
        )
        val replacement = reserveUse("reserve-use-2", "movement-1", "60.00", now)

        assertTrue(repository.record(first).isSuccess)
        assertTrue(repository.record(reversal).isSuccess)
        assertTrue(repository.record(replacement).isSuccess)
        assertTrue(repository.record(replacement.copy(id = "reserve-use-3")).isFailure)
        assertEquals(3, repository.observeAll().first().size)
    }

    private fun reserveUse(
        id: String,
        movementId: String,
        amount: String,
        now: Instant,
    ) = ReserveEvent(
        id = id,
        type = ReserveEventType.USE,
        amount = Money.of(BigDecimal(amount)).getOrError(),
        sourceMovementId = movementId,
        occurredAt = now,
        createdAt = now,
    )
}
