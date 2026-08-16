package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.EnvelopePlanRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import java.time.Instant

class CreateEnvelopeUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val envelopeRepository = FakeEnvelopeRepository()
    private val categoryRepository = FakeCategoryRepository()
    private val financialPlanRepository = FakeFinancialPlanRepository()
    private val envelopePlanRepository = FakeEnvelopePlanRepository()
    private val useCase = CreateEnvelopeUseCase(
        envelopeRepository = envelopeRepository,
        categoryRepository = categoryRepository,
        financialPlanRepository = financialPlanRepository,
        envelopePlanRepository = envelopePlanRepository,
        timeProvider = FixedTimeProvider(now),
    )

    @Test
    fun createsEnvelopeAndLinksToPlan() = runTest {
        categoryRepository.categories = listOf(
            Category(id = CategoryIds.OTHER, name = "Otros"),
        )

        val result = useCase(
            name = "Regalos",
            categoryId = CategoryIds.OTHER,
            weeklyLimit = Money.of(BigDecimal("50.00")).getOrError(),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, envelopePlanRepository.saves.size)
        assertEquals("envelope-${now.toEpochMilli()}", envelopePlanRepository.saves.first().first.id)
        assertTrue(
            envelopePlanRepository.saves.first().second
                ?.envelopeIds
                .orEmpty()
                .contains("envelope-${now.toEpochMilli()}"),
        )
    }

    @Test
    fun rejectsDuplicateCategory() = runTest {
        categoryRepository.categories = listOf(
            Category(id = CategoryIds.OTHER, name = "Otros"),
        )
        envelopeRepository.envelopes.value = listOf(
            Envelope(
                id = "envelope-existing",
                name = "Existente",
                weeklyLimit = Money.of(BigDecimal("30.00")).getOrError(),
                categoryId = CategoryIds.OTHER,
            ),
        )

        val result = useCase(
            name = "Otro",
            categoryId = CategoryIds.OTHER,
            weeklyLimit = Money.of(BigDecimal("40.00")).getOrError(),
        )

        assertTrue(result.isFailure)
    }

    private class FakeEnvelopeRepository : EnvelopeRepository {
        val envelopes = MutableStateFlow<List<Envelope>>(emptyList())

        override fun observeEnvelopes() = envelopes

        override suspend fun getById(id: String): Envelope? =
            envelopes.value.find { it.id == id }

        override suspend fun save(envelope: Envelope): Result<Unit> {
            envelopes.value = envelopes.value.filterNot { it.id == envelope.id } + envelope
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCategoryRepository : CategoryRepository {
        var categories: List<Category> = emptyList()

        override fun observeCategories() = MutableStateFlow(categories)

        override suspend fun getById(id: String): Category? = categories.find { it.id == id }

        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeFinancialPlanRepository : FinancialPlanRepository {
        private var plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("3000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("1800.00")).getOrError(),
            envelopeIds = emptyList(),
        )

        override fun observePlans() = MutableStateFlow(listOf(plan))

        override suspend fun getById(id: String): FinancialPlan? =
            if (id == plan.id) plan else null

        override suspend fun save(plan: FinancialPlan): Result<Unit> {
            this.plan = plan
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeEnvelopePlanRepository : EnvelopePlanRepository {
        val saves = mutableListOf<Pair<Envelope, FinancialPlan?>>()

        override suspend fun saveEnvelopeWithPlan(
            envelope: Envelope,
            plan: FinancialPlan?,
        ): Result<Unit> {
            saves += envelope to plan
            return Result.success(Unit)
        }

        override suspend fun deleteEnvelopeWithPlan(
            envelopeId: String,
            plan: FinancialPlan?,
        ): Result<Unit> = Result.success(Unit)
    }
}

private fun <T> pe.kipu.core.domain.model.DomainResult<T>.getOrError(): T = when (this) {
    is pe.kipu.core.domain.model.DomainResult.Ok -> value
    is pe.kipu.core.domain.model.DomainResult.Err -> error("Expected Ok")
}
