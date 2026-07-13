package pe.kipu.core.data.repository

import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.PlanSetup

class PlanSetupSaveExecutorTest {
    @Test
    fun `rejects duplicate category ids before persistence`() = runTest {
        val setup = validSetup().copy(
            categories = listOf(
                Category("category-custom", "Mascota"),
                Category("category-custom", "Salud"),
            ),
        )
        var persisted = false

        val result = executePlanSetupSave(setup) { persisted = true }

        assertTrue(result.isFailure)
        assertTrue(!persisted)
    }

    @Test
    fun `rejects duplicate category names ignoring case and whitespace`() = runTest {
        val setup = validSetup().copy(
            categories = listOf(
                Category("category-one", "Mascota"),
                Category("category-two", " mascota "),
            ),
        )

        val result = executePlanSetupSave(setup) { error("must not persist") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects duplicate envelope ids before persistence`() = runTest {
        val envelope = envelope()
        val setup = validSetup().copy(envelopes = listOf(envelope, envelope.copy(name = "Otro")))

        val result = executePlanSetupSave(setup) { error("must not persist") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects duplicate commitment ids before persistence`() = runTest {
        val commitment = commitment()
        val setup = validSetup().copy(
            commitmentsToSave = listOf(commitment, commitment.copy(title = "Otra meta")),
        )

        val result = executePlanSetupSave(setup) { error("must not persist") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects intersection between commitments to save and settle`() = runTest {
        val setup = validSetup().copy(
            commitmentsToSave = listOf(commitment()),
            commitmentIdsToSettle = setOf("commitment-goal"),
        )

        val result = executePlanSetupSave(setup) { error("must not persist") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects plan and envelope identity mismatch`() = runTest {
        val setup = validSetup().copy(
            plan = plan(envelopeIds = listOf("envelope-other")),
        )

        val result = executePlanSetupSave(setup) { error("must not persist") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects structurally invalid models`() = runTest {
        val setup = validSetup().copy(categories = listOf(Category("category-custom", "")))

        val result = executePlanSetupSave(setup) { error("must not persist") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `passes the original setup without generating identities`() = runTest {
        val setup = validSetup()
        var received: PlanSetup? = null

        val result = executePlanSetupSave(setup) { received = it }

        assertTrue(result.isSuccess)
        assertSame(setup, received)
    }

    @Test
    fun `returns the original persistence failure`() = runTest {
        val expected = IllegalStateException("database failure")

        val result = executePlanSetupSave(validSetup()) { throw expected }

        assertSame(expected, result.exceptionOrNull())
    }

    @Test
    fun `rethrows cancellation exception`() = runTest {
        val expected = CancellationException("cancelled")

        val thrown = runCatching {
            executePlanSetupSave(validSetup()) { throw expected }
        }.exceptionOrNull()

        assertSame(expected, thrown)
    }

    private fun validSetup(): PlanSetup = PlanSetup(
        plan = plan(listOf("envelope-custom")),
        categories = listOf(Category("category-custom", "Mascota")),
        envelopes = listOf(envelope()),
        commitmentsToSave = emptyList(),
        commitmentIdsToSettle = emptySet(),
    )

    private fun plan(envelopeIds: List<String>): FinancialPlan = FinancialPlan(
        id = "plan-primary",
        estimatedMonthlyIncome = money("2000"),
        fixedExpenses = money("500"),
        envelopeIds = envelopeIds,
        budgetCycle = BudgetCycle.WEEKLY,
    )

    private fun envelope(): Envelope = Envelope(
        id = "envelope-custom",
        name = "Mascota",
        weeklyLimit = money("50"),
        categoryId = "category-custom",
    )

    private fun commitment(): Commitment = Commitment(
        id = "commitment-goal",
        type = CommitmentType.SAVINGS_GOAL,
        title = "Fondo",
        targetAmount = money("1000"),
    )

    private fun money(value: String): Money = Money.of(value.toBigDecimal()).getOrError()
}
