package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider

class MarkServiceReceiptPaidUseCaseTest {
    private val savedReceipts = mutableListOf<MonthlyServiceReceipt>()
    private val receiptRepository = object : MonthlyServiceReceiptRepository {
        override fun observeReceiptsForMonth(monthKey: String) = flowOf(savedReceipts)
        override fun observeAllPaidMovementIds() = flowOf(emptySet<String>())
        override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) { savedReceipts += receipt }
        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String) = null
    }
    private val failingMovementRepository = object : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())
        override suspend fun getById(id: EntityId) = null
        override suspend fun findByCounterpartyName(counterpartyName: String) = emptyList<Movement>()
        override suspend fun save(movement: Movement) = Result.failure<Unit>(IllegalStateException("save failed"))
        override suspend fun delete(id: EntityId) = Result.success(Unit)
    }
    private val auditRepository = object : MovementAuditRepository {
        override fun observeAuditLogs() = flowOf(emptyList<MovementAuditEntry>())
        override suspend fun recordAudit(entry: MovementAuditEntry) = Result.success(Unit)
        override suspend fun getAll() = emptyList<MovementAuditEntry>()
    }

    @Test
    fun `failed movement save does not mark receipt paid`() = runTest {
        val receipt = MonthlyServiceReceipt(
            key = ServiceReceiptKey.LIGHT,
            title = "Luz",
            configuredAmount = Money.of(BigDecimal("60.00")).getOrError(),
            monthKey = "2026-08",
        )
        val useCase = MarkServiceReceiptPaidUseCase(
            monthlyServiceReceiptRepository = receiptRepository,
            timeProvider = TimeProvider { Instant.parse("2026-08-23T10:00:00Z") },
        )

        runCatching { useCase(receipt) }

        assertTrue(savedReceipts.isEmpty())
    }
}
