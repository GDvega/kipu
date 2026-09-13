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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.seed.DefaultCategorySeed
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.*
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.core.domain.usecase.CalculateReserveBalanceUseCase
import pe.kipu.core.domain.usecase.ResolveDuplicateMovementUseCase

@RunWith(AndroidJUnit4::class)
class RoomDuplicateMergeIntegrityInstrumentedTest {
    private lateinit var database: KipuDatabase
    private lateinit var movements: RoomMovementRepository
    private lateinit var reserve: RoomReserveEventRepository
    private lateinit var receipts: RoomMonthlyServiceReceiptRepository
    private val now = Instant.parse("2026-09-09T15:00:00Z")

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, KipuDatabase::class.java,
        ).build()
        database.categoryDao().insertAll(DefaultCategorySeed.categories)
        movements = RoomMovementRepository(database)
        reserve = RoomReserveEventRepository(database.reserveEventDao())
        receipts = RoomMonthlyServiceReceiptRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun mergeTransfersReserveReceiptAndEnvelopeFromNewerDuplicate() = runBlocking {
        val older = movement("older")
        val newer = movement("newer").copy(createdAt = now.plusSeconds(1), envelopeId = "service-envelope")
        movements.save(older).getOrThrow()
        movements.save(newer).getOrThrow()
        reserve.record(event("contribution", ReserveEventType.CONTRIBUTION, "100")).getOrThrow()
        reserve.record(event("use-newer", ReserveEventType.USE, "50", newer.id)).getOrThrow()
        receipts.saveReceipt(receipt(newer.id))

        val result = ResolveDuplicateMovementUseCase(movements)(pair(older, newer), DuplicateResolution.MERGE)

        assertTrue(result.isSuccess)
        assertNull(movements.getById(newer.id))
        assertEquals("service-envelope", movements.getById(older.id)?.envelopeId)
        assertEquals(older.id, receipts.getReceipt("2026-09", "LIGHT")?.paidMovementId)
        assertTrue(requireNotNull(receipts.getReceipt("2026-09", "LIGHT")).isPaid)
        val events = reserve.observeAll().first()
        val reversedIds = events.mapNotNull { it.reversesEventId }.toSet()
        val activeUses = events.filter { it.type == ReserveEventType.USE && it.id !in reversedIds }
        assertEquals(1, activeUses.size)
        assertEquals(older.id, activeUses.single().sourceMovementId)
        assertEquals(BigDecimal("50.00"), CalculateReserveBalanceUseCase()(events).balance)
    }

    @Test
    fun conflictingEnvelopeLinksRejectMergeWithoutDeletingEitherMovement() = runBlocking {
        val older = movement("older").copy(envelopeId = "first-envelope")
        val newer = movement("newer").copy(createdAt = now.plusSeconds(1), envelopeId = "second-envelope")
        movements.save(older).getOrThrow()
        movements.save(newer).getOrThrow()

        val result = ResolveDuplicateMovementUseCase(movements)(pair(older, newer), DuplicateResolution.MERGE)

        assertTrue(result.isFailure)
        assertEquals(listOf(older, newer).toSet(), movements.observeMovements().first().toSet())
    }

    @Test
    fun changedMovementRejectsStaleDuplicateConfirmation() = runBlocking {
        val older = movement("older")
        val newer = movement("newer").copy(createdAt = now.plusSeconds(1))
        movements.save(older).getOrThrow()
        movements.save(newer.copy(amount = money("80"))).getOrThrow()

        val result = ResolveDuplicateMovementUseCase(movements)(pair(older, newer), DuplicateResolution.MERGE)

        assertTrue(result.isFailure)
        assertFalse(movements.observeMovements().first().isEmpty())
        assertEquals(money("80"), movements.getById(newer.id)?.amount)
    }

    @Test
    fun twoReserveUsesKeepOnlyTheLargestAndRepeatedMergeDoesNotWriteAgain() = runBlocking {
        val older = movement("older")
        val newer = movement("newer").copy(createdAt = now.plusSeconds(1))
        movements.save(older).getOrThrow()
        movements.save(newer).getOrThrow()
        reserve.record(event("contribution", ReserveEventType.CONTRIBUTION, "100")).getOrThrow()
        reserve.record(event("use-older", ReserveEventType.USE, "30", older.id)).getOrThrow()
        reserve.record(event("use-newer", ReserveEventType.USE, "50", newer.id)).getOrThrow()
        val resolve = ResolveDuplicateMovementUseCase(movements)
        assertTrue(resolve(pair(older, newer), DuplicateResolution.MERGE).isSuccess)
        val events = reserve.observeAll().first()
        assertEquals(BigDecimal("50.00"), CalculateReserveBalanceUseCase()(events).balance)
        assertTrue(resolve(pair(older, newer), DuplicateResolution.MERGE).isFailure)
        assertEquals(events, reserve.observeAll().first())
        assertEquals(1, movements.observeMovements().first().size)
    }

    @Test
    fun refundRequiresManualReviewAndLeavesAllDataUntouched() = runBlocking {
        val older = movement("older")
        val newer = movement("newer").copy(createdAt = now.plusSeconds(1))
        movements.save(older).getOrThrow()
        movements.save(newer).getOrThrow()
        reserve.record(event("use", ReserveEventType.USE, "50", newer.id)).getOrThrow()
        reserve.record(event("refund", ReserveEventType.REFUND, "10", newer.id)).getOrThrow()
        receipts.saveReceipt(receipt(newer.id))
        val events = reserve.observeAll().first()
        assertTrue(ResolveDuplicateMovementUseCase(movements)(pair(older, newer), DuplicateResolution.MERGE).isFailure)
        assertEquals(events, reserve.observeAll().first())
        assertEquals(newer.id, receipts.getReceipt("2026-09", "LIGHT")?.paidMovementId)
        assertEquals(setOf(older, newer), movements.observeMovements().first().toSet())
    }

    private fun pair(a: Movement, b: Movement) = MovementDuplicatePair(a, b, "duplicate_amount_counterparty_time")
    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()
    private fun movement(id: String) = Movement(
        id = id, type = MovementType.EXPENSE, amount = money("55"), categoryId = CategoryIds.SERVICES,
        channel = PaymentChannel.CASH, source = MovementSource.MANUAL, status = MovementStatus.CONFIRMED,
        recordedAt = now, createdAt = now,
    )
    private fun event(id: String, type: ReserveEventType, amount: String, movementId: String? = null) =
        ReserveEvent(id, type, money(amount), sourceMovementId = movementId, occurredAt = now, createdAt = now)
    private fun receipt(movementId: String) = MonthlyServiceReceipt(
        ServiceReceiptKey.LIGHT, "Luz", money("45"), "2026-09", true, movementId, now,
    )
}
