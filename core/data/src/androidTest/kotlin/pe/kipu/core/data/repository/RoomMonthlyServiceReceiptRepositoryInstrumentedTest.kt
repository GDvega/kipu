package pe.kipu.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.seed.DefaultCategorySeed
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey

@RunWith(AndroidJUnit4::class)
class RoomMonthlyServiceReceiptRepositoryInstrumentedTest {
    private lateinit var database: KipuDatabase
    private lateinit var repository: RoomMonthlyServiceReceiptRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking { database.categoryDao().insertAll(DefaultCategorySeed.categories) }
        repository = RoomMonthlyServiceReceiptRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun markPaidRollsBackMovementAndReceiptWhenAuditWriteFails() = runBlocking {
        val now = Instant.parse("2026-08-23T10:00:00Z")
        val movement = Movement(
            id = "movement-atomic",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("60.00")).getOrError(),
            categoryId = CategoryIds.SERVICES,
            channel = PaymentChannel.CASH,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = now,
            createdAt = now,
        )
        val receipt = MonthlyServiceReceipt(
            key = ServiceReceiptKey.LIGHT,
            title = "Luz",
            configuredAmount = movement.amount,
            monthKey = "2026-08",
            isPaid = true,
            paidMovementId = movement.id,
            paidAt = now,
        )
        val audit = MovementAuditEntry(
            id = "audit-overflow",
            movementId = movement.id,
            action = MovementAuditAction.CREATED,
            movementType = movement.type,
            amount = Money.of(BigDecimal("1000000000000000000000000000000")).getOrError(),
            categoryId = movement.categoryId,
            channel = movement.channel,
            timestamp = now,
        )

        val result = repository.markPaid(receipt, movement, audit)

        assertTrue(result.isFailure)
        assertNull(database.movementDao().getById(movement.id))
        assertNull(database.monthlyServiceReceiptDao().getByKey("2026-08", "LIGHT"))
        assertTrue(database.movementAuditDao().getAll().isEmpty())
    }
}
