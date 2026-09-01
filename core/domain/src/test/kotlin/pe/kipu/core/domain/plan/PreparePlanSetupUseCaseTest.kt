package pe.kipu.core.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase

class PreparePlanSetupUseCaseTest {
    private val useCase = PreparePlanSetupUseCase(ValidateFinancialPlanUseCase())

    @Test
    fun `prepares one coherent immutable setup used by validation`() {
        val result = useCase(validInput())

        assertTrue(result is PlanSetupPreparationResult.Success)
        val success = result as PlanSetupPreparationResult.Success
        assertEquals(success.setup.envelopes.map { it.id }, success.setup.plan.envelopeIds)
        assertEquals(
            ValidateFinancialPlanUseCase()(
                success.setup.plan,
                success.setup.envelopes,
                success.setup.commitmentsToSave,
            ),
            success.validation,
        )
        assertEquals(money("20"), success.setup.plan.antSpendingLimit)
        assertEquals(money("75"), success.setup.plan.reserveMonthlyContribution)
        assertFalse(success.setup.plan.antSpendingAlertEnabled)
        assertEquals(75, success.setup.plan.antSpendingAlertPercent)
        assertEquals(
            setOf(CategoryIds.FOOD, CategoryIds.TRANSPORT),
            success.setup.plan.antSpendingTrackedCategoryIds,
        )
    }

    @Test
    fun `custom envelope keeps definitive category and envelope identities`() {
        val line = PlanWizardLineItem(id = "line-stable", label = "Mascota", amountText = "25")

        val success = useCase(validInput(customEnvelopeLines = listOf(line))) as PlanSetupPreparationResult.Success

        val category = success.setup.categories.single { it.name == "Mascota" }
        val envelope = success.setup.envelopes.single { it.name == "Mascota" }
        assertEquals("category-plan-line-stable", category.id)
        assertEquals("envelope-plan-line-stable", envelope.id)
        assertEquals(category.id, envelope.categoryId)
    }

    @Test
    fun `invalid custom amount returns typed error`() {
        val result = useCase(
            validInput(customEnvelopeLines = listOf(PlanWizardLineItem("line-1", "Mascota", "abc"))),
        )

        assertEquals(
            PlanSetupPreparationError.InvalidCustomEnvelopeAmount("line-1"),
            (result as PlanSetupPreparationResult.Error).reason,
        )
    }

    @Test
    fun `blank custom name returns typed error`() {
        val result = useCase(
            validInput(customEnvelopeLines = listOf(PlanWizardLineItem("line-1", "  ", "20"))),
        )

        assertEquals(
            PlanSetupPreparationError.BlankCustomEnvelopeName("line-1"),
            (result as PlanSetupPreparationResult.Error).reason,
        )
    }

    @Test
    fun `duplicate custom names are rejected ignoring case and whitespace`() {
        val result = useCase(
            validInput(
                customEnvelopeLines = listOf(
                    PlanWizardLineItem("line-1", "Mascota", "20"),
                    PlanWizardLineItem("line-2", " mascota ", "30"),
                ),
            ),
        )

        assertEquals(
            PlanSetupPreparationError.DuplicateCustomEnvelopeName,
            (result as PlanSetupPreparationResult.Error).reason,
        )
    }

    @Test
    fun `duplicate custom line identities are rejected explicitly`() {
        val result = useCase(
            validInput(
                customEnvelopeLines = listOf(
                    PlanWizardLineItem("line-1", "Mascota", "20"),
                    PlanWizardLineItem("line-1", "Salud", "30"),
                ),
            ),
        )

        assertEquals(
            PlanSetupPreparationError.DuplicateCustomEnvelopeIdentity,
            (result as PlanSetupPreparationResult.Error).reason,
        )
    }

    @Test
    fun `skipping existing active wizard goal requests settlement`() {
        val result = useCase(
            validInput(goalSkipped = true, existingCommitments = listOf(existingGoal())),
        ) as PlanSetupPreparationResult.Success

        assertEquals(setOf(CommitmentIds.EMERGENCY_FUND), result.setup.commitmentIdsToSettle)
        assertTrue(result.setup.commitmentsToSave.none { it.id == CommitmentIds.EMERGENCY_FUND })
    }

    @Test
    fun `skipping without previous goal creates no goal operation`() {
        val result = useCase(validInput(goalSkipped = true)) as PlanSetupPreparationResult.Success

        assertTrue(result.setup.commitmentIdsToSettle.isEmpty())
        assertTrue(result.setup.commitmentsToSave.none { it.id == CommitmentIds.EMERGENCY_FUND })
    }

    @Test
    fun `reactivating goal reuses wizard id and clears settled state`() {
        val result = useCase(
            validInput(goalSkipped = false, existingCommitments = listOf(existingGoal(isSettled = true))),
        ) as PlanSetupPreparationResult.Success

        val goal = result.setup.commitmentsToSave.single { it.id == CommitmentIds.EMERGENCY_FUND }
        assertEquals(CommitmentIds.EMERGENCY_FUND, goal.id)
        assertFalse(goal.isSettled)
        assertTrue(result.setup.commitmentIdsToSettle.isEmpty())
    }

    @Test
    fun `equivalent retries preserve identities`() {
        val input = validInput(
            customEnvelopeLines = listOf(PlanWizardLineItem("line-stable", "Mascota", "25")),
        )

        val first = (useCase(input) as PlanSetupPreparationResult.Success).setup
        val second = (useCase(input) as PlanSetupPreparationResult.Success).setup

        assertEquals(first, second)
    }

    @Test
    fun `financially invalid prepared plan returns typed validation error`() {
        val result = useCase(
            validInput().copy(
                estimatedMonthlyIncome = money("100"),
                envelopeLimits = mapOf(DefaultPlanEnvelopeIds.FOOD to "100"),
            ),
        )

        val reason = (result as PlanSetupPreparationResult.Error).reason
        assertTrue(reason is PlanSetupPreparationError.InvalidFinancialPlan)
    }

    @Test
    fun `existing category identity is reused by normalized name`() {
        val input = validInput(
            existingCategories = listOf(Category("category-pets", "Mascota")),
            customEnvelopeLines = listOf(PlanWizardLineItem("line-stable", " mascota ", "25")),
        )

        val setup = (useCase(input) as PlanSetupPreparationResult.Success).setup

        assertTrue(setup.categories.none { it.id == "category-plan-line-stable" })
        assertEquals("category-pets", setup.envelopes.single { it.name == "mascota" }.categoryId)
    }

    @Test
    fun `omitted wizard custom envelope is removed but manual envelope is retained`() {
        val removed = envelope("envelope-plan-old", "Mascota", "category-pets", "25")
        val manual = envelope("envelope-manual", "Salud", "category-health", "30")

        val setup = (
            useCase(validInput(existingEnvelopes = listOf(removed, manual)))
                as PlanSetupPreparationResult.Success
            ).setup

        assertEquals(setOf(removed.id), setup.envelopeIdsToDelete)
        assertTrue(setup.envelopes.none { it.id == removed.id })
        assertTrue(setup.envelopes.any { it.id == manual.id })
        assertTrue(removed.id !in setup.plan.envelopeIds)
    }

    @Test
    fun `preparation is pure and keeps supplied collections unchanged`() {
        val categories = mutableListOf(Category("category-pets", "Mascota"))
        val envelopes = mutableListOf(envelope("existing", "Existente", CategoryIds.OTHER, "10"))
        val commitments = mutableListOf(existingGoal())
        val input = validInput(
            existingCategories = categories,
            existingEnvelopes = envelopes,
            existingCommitments = commitments,
        )

        useCase(input)

        assertEquals(1, categories.size)
        assertEquals(1, envelopes.size)
        assertEquals(1, commitments.size)
    }

    private fun validInput(
        goalSkipped: Boolean = true,
        customEnvelopeLines: List<PlanWizardLineItem> = emptyList(),
        existingCategories: List<Category> = emptyList(),
        existingEnvelopes: List<Envelope> = emptyList(),
        existingCommitments: List<Commitment> = emptyList(),
    ): PlanSetupPreparationInput = PlanSetupPreparationInput(
        estimatedMonthlyIncome = money("2000"),
        fixedExpenses = money("100"),
        initialBalance = Money.ZERO,
        reserveMonthlyContribution = money("75"),
        budgetCycle = BudgetCycle.WEEKLY,
        envelopeLimits = mapOf(DefaultPlanEnvelopeIds.FOOD to "100"),
        antSpendingLimitText = "20",
        antSpendingAlertEnabled = false,
        antSpendingAlertPercent = 75,
        antSpendingTrackedCategoryIds = setOf(CategoryIds.FOOD, CategoryIds.TRANSPORT),
        customEnvelopeLines = customEnvelopeLines,
        goalSkipped = goalSkipped,
        goalTitle = "Fondo de emergencia",
        goalTargetText = "500",
        goalCurrentText = "50",
        goalMonthsText = "5",
        existingCategories = existingCategories,
        existingEnvelopes = existingEnvelopes,
        existingCommitments = existingCommitments,
    )

    private fun existingGoal(isSettled: Boolean = false): Commitment = Commitment(
        id = CommitmentIds.EMERGENCY_FUND,
        type = CommitmentType.SAVINGS_GOAL,
        title = "Meta anterior",
        targetAmount = money("300"),
        currentAmount = money("20"),
        isSettled = isSettled,
    )

    private fun envelope(id: String, name: String, categoryId: String, amount: String) = Envelope(
        id = id,
        name = name,
        categoryId = categoryId,
        weeklyLimit = money(amount),
    )

    private fun money(value: String): Money = Money.of(value.toBigDecimal()).getOrError()
}
