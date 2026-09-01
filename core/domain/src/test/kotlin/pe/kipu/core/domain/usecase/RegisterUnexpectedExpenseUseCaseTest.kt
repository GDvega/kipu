package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.LocalTransactionRunner
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.time.TimeProvider

class RegisterUnexpectedExpenseUseCaseTest {
    @Test
    fun `registers purchase and reserve use inside one transaction`() = runTest {
        val transactionRunner = CountingTransactionRunner()
        val movements = RecordingMovementRepository()
        val reserves = RecordingReserveRepository()
        val create = CreateManualMovementUseCase(
            movements,
            RecordingAuditRepository(),
            FixedTimeProvider,
            reserves,
            transactionRunner,
        )
        val useCase = RegisterUnexpectedExpenseUseCase(
            create,
            ApplyRecoveryPlanUseCase(RecordingEnvelopeRepository(), transactionRunner),
            transactionRunner,
        )

        val result = useCase(
            amount = money("300.00"),
            categoryId = CategoryIds.OTHER,
            channel = PaymentChannel.CASH,
            reserveAmount = money("100.00"),
            recoveryPlan = UnexpectedExpenseRecoveryPlan(emptyList(), money("0.00"), true),
        )

        assertTrue(result.isSuccess)
        assertEquals(money("300.00"), movements.saved.single().amount)
        assertEquals(money("100.00"), reserves.recorded.single().amount)
        assertTrue(transactionRunner.invocations >= 2)
    }

    private object FixedTimeProvider : TimeProvider {
        override fun now(): Instant = Instant.parse("2026-08-29T12:00:00Z")
    }

    private class CountingTransactionRunner : LocalTransactionRunner {
        var invocations = 0
        override suspend fun <T> run(block: suspend () -> T): Result<T> = runCatching {
            invocations++
            block()
        }
    }

    private class RecordingMovementRepository : MovementRepository {
        val saved = mutableListOf<Movement>()
        override fun observeMovements(): Flow<List<Movement>> = flowOf(saved)
        override suspend fun getById(id: String): Movement? = saved.find { it.id == id }
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit).also { saved += movement }
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class RecordingAuditRepository : MovementAuditRepository {
        override fun observeAuditLogs(): Flow<List<MovementAuditEntry>> = flowOf(emptyList())
        override suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit> = Result.success(Unit)
        override suspend fun getAll(): List<MovementAuditEntry> = emptyList()
    }

    private class RecordingReserveRepository : ReserveEventRepository {
        val recorded = mutableListOf<ReserveEvent>()
        override fun observeAll(): Flow<List<ReserveEvent>> = flowOf(recorded)
        override suspend fun getById(id: String): ReserveEvent? = recorded.find { it.id == id }
        override suspend fun record(event: ReserveEvent): Result<Unit> = Result.success(Unit).also { recorded += event }
    }

    private class RecordingEnvelopeRepository : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(emptyList())
        override suspend fun getById(id: String): Envelope? = null
        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()
}
