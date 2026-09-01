package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.time.TimeProvider

class ContributeMonthlyReserveUseCaseTest {
    private val now = Instant.parse("2026-08-27T15:00:00Z")
    private val repository = FakeReserveEventRepository()
    private val useCase = ContributeMonthlyReserveUseCase(repository, TimeProvider { now })

    @Test
    fun `records the monthly target once and leaves cash movements untouched`() = runTest {
        val amount = Money.of(BigDecimal("200.00")).getOrError()

        assertTrue(useCase(amount).getOrThrow())
        assertEquals(false, useCase(amount).getOrThrow())

        assertEquals(1, repository.events.value.size)
        assertEquals(ReserveEventType.CONTRIBUTION, repository.events.value.single().type)
        assertEquals(amount, repository.events.value.single().amount)
    }

    @Test
    fun `a reversed contribution allows a replacement in the same month`() = runTest {
        val amount = Money.of(BigDecimal("100.00")).getOrError()
        assertTrue(useCase(amount).getOrThrow())
        val first = repository.events.value.single()
        repository.record(
            ReserveEvent(
                id = "reversal",
                type = ReserveEventType.REVERSAL,
                amount = first.amount,
                reversesEventId = first.id,
                occurredAt = now,
                createdAt = now,
            ),
        ).getOrThrow()

        assertTrue(useCase(amount).getOrThrow())
        assertEquals(3, repository.events.value.size)
    }

    private class FakeReserveEventRepository : ReserveEventRepository {
        val events = MutableStateFlow<List<ReserveEvent>>(emptyList())

        override fun observeAll(): Flow<List<ReserveEvent>> = events

        override suspend fun getById(id: String): ReserveEvent? = events.value.find { it.id == id }

        override suspend fun record(event: ReserveEvent): Result<Unit> {
            events.value = events.value + event
            return Result.success(Unit)
        }
    }
}
