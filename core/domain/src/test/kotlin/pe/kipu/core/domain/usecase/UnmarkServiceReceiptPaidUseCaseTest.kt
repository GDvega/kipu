package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.core.domain.receipt.ServiceReceiptType
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import java.math.BigDecimal
import java.time.Instant

class UnmarkServiceReceiptPaidUseCaseTest {

    private val fixedNow = Instant.parse("2026-08-23T10:00:00Z")
    private val timeProvider = TimeProvider { fixedNow }

    private val fakeReceipts = mutableMapOf<String, MonthlyServiceReceipt>()
    private val fakeMovements = mutableMapOf<String, Movement>()
    private val recordedAudits = mutableListOf<MovementAuditEntry>()
    private var failMovementDelete = false

    private val receiptRepo = object : MonthlyServiceReceiptRepository {
        override fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>> =
            flowOf(fakeReceipts.values.toList())

        override fun observeAllPaidMovementIds(): Flow<Set<String>> =
            flowOf(fakeReceipts.values.mapNotNull { it.paidMovementId }.toSet())

        override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) {
            fakeReceipts["${receipt.monthKey}-${receipt.key.identifier}"] = receipt
        }

        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt? =
            fakeReceipts["$monthKey-$serviceKeyIdentifier"]

        override suspend fun unmarkPaid(
            receipt: MonthlyServiceReceipt,
            movementId: String?,
            auditEntry: MovementAuditEntry?,
        ): Result<Unit> {
            if (failMovementDelete) return Result.failure(IllegalStateException("delete failed"))
            movementId?.let(fakeMovements::remove)
            fakeReceipts["${receipt.monthKey}-${receipt.key.identifier}"] = receipt
            auditEntry?.let(recordedAudits::add)
            return Result.success(Unit)
        }
    }

    private val movementRepo = object : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(fakeMovements.values.toList())
        override suspend fun getById(id: EntityId): Movement? = fakeMovements[id]
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> {
            fakeMovements[movement.id] = movement
            return Result.success(Unit)
        }
        override suspend fun delete(id: EntityId): Result<Unit> {
            if (failMovementDelete) return Result.failure(IllegalStateException("delete failed"))
            fakeMovements.remove(id)
            return Result.success(Unit)
        }
    }

    private val auditRepo = object : MovementAuditRepository {
        override fun observeAuditLogs(): Flow<List<MovementAuditEntry>> = flowOf(recordedAudits)
        override suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit> {
            recordedAudits.add(entry)
            return Result.success(Unit)
        }
        override suspend fun getAll(): List<MovementAuditEntry> = recordedAudits
    }

    private val unmarkUseCase = UnmarkServiceReceiptPaidUseCase(
        monthlyServiceReceiptRepository = receiptRepo,
        movementRepository = movementRepo,
        timeProvider = timeProvider,
    )

    @Test
    fun `unmarking paid receipt deletes created movement and updates receipt to unpaid`() = runTest {
        val movement = Movement(
            id = "mov-light-1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("60.00")).getOrError(),
            categoryId = CategoryIds.SERVICES,
            channel = PaymentChannel.CASH,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            description = "Pago Luz",
            recordedAt = fixedNow,
            createdAt = fixedNow,
        )
        fakeMovements[movement.id] = movement

        val paidReceipt = MonthlyServiceReceipt(
            key = ServiceReceiptKey.LIGHT,
            title = "Luz",
            configuredAmount = Money.of(BigDecimal("60.00")).getOrError(),
            monthKey = "2026-08",
            isPaid = true,
            paidMovementId = movement.id,
            paidAt = fixedNow,
        )
        fakeReceipts["2026-08-${paidReceipt.key.identifier}"] = paidReceipt

        unmarkUseCase(paidReceipt)

        val updated = fakeReceipts["2026-08-${paidReceipt.key.identifier}"]
        assertTrue(updated != null)
        assertFalse(updated!!.isPaid)
        assertNull(updated.paidMovementId)
        assertNull(updated.paidAt)

        assertNull(fakeMovements["mov-light-1"])

        assertEquals(1, recordedAudits.size)
        val audit = recordedAudits.first()
        assertEquals(MovementAuditAction.DELETED, audit.action)
        assertEquals("mov-light-1", audit.movementId)
        assertEquals("Luz", audit.counterpartyName)
    }

    @Test
    fun `unmarking receipt without paidMovementId safely resets receipt without errors`() = runTest {
        val receipt = MonthlyServiceReceipt(
            key = ServiceReceiptKey.WATER,
            title = "Agua",
            configuredAmount = Money.of(BigDecimal("40.00")).getOrError(),
            monthKey = "2026-08",
            isPaid = true,
            paidMovementId = null,
            paidAt = fixedNow,
        )
        fakeReceipts["2026-08-${receipt.key.identifier}"] = receipt

        unmarkUseCase(receipt)

        val updated = fakeReceipts["2026-08-${receipt.key.identifier}"]
        assertTrue(updated != null)
        assertFalse(updated!!.isPaid)
        assertNull(updated.paidMovementId)
        assertNull(updated.paidAt)
        assertEquals(0, recordedAudits.size)
    }

    @Test
    fun `failed movement deletion keeps receipt paid`() = runTest {
        val movement = Movement(
            id = "mov-light-failure",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("60.00")).getOrError(),
            categoryId = CategoryIds.SERVICES,
            channel = PaymentChannel.CASH,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = fixedNow,
            createdAt = fixedNow,
        )
        val receipt = MonthlyServiceReceipt(
            key = ServiceReceiptKey.LIGHT,
            title = "Luz",
            configuredAmount = movement.amount,
            monthKey = "2026-08",
            isPaid = true,
            paidMovementId = movement.id,
            paidAt = fixedNow,
        )
        fakeMovements[movement.id] = movement
        fakeReceipts["2026-08-${receipt.key.identifier}"] = receipt
        failMovementDelete = true

        val result = runCatching { unmarkUseCase(receipt) }

        assertTrue(result.isFailure)
        assertTrue(fakeReceipts["2026-08-${receipt.key.identifier}"]!!.isPaid)
        assertTrue(fakeMovements.containsKey(movement.id))
        assertTrue(recordedAudits.isEmpty())
    }
}
