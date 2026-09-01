package pe.kipu.feature.home.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.usecase.AnalyzeVoiceIntentUseCase
import pe.kipu.core.domain.usecase.ApplyRecoveryPlanUseCase
import pe.kipu.core.domain.usecase.BuildUnexpectedExpenseRecoveryPlanUseCase
import pe.kipu.core.domain.usecase.CalculateUnexpectedExpenseCoverageUseCase
import pe.kipu.core.domain.usecase.CalculateCashFlowSummaryUseCase
import pe.kipu.core.domain.usecase.CalculateCategoryPeriodSpentUseCase
import pe.kipu.core.domain.usecase.CalculateCycleAvailableUseCase
import pe.kipu.core.domain.usecase.CalculateEnvelopeBudgetStateUseCase
import pe.kipu.core.domain.usecase.CalculatePeriodEnvelopeTotalsUseCase
import pe.kipu.core.domain.usecase.CreateManualMovementUseCase
import pe.kipu.core.domain.usecase.ContributeMonthlyReserveUseCase
import pe.kipu.core.domain.usecase.DetectAntSpendingUseCase
import pe.kipu.core.domain.usecase.DetectAntSpendingWeeklyLimitUseCase
import pe.kipu.core.domain.usecase.MarkServiceReceiptPaidUseCase
import pe.kipu.core.domain.usecase.UnmarkServiceReceiptPaidUseCase
import pe.kipu.core.domain.usecase.ObserveEnvelopeBudgetsUseCase
import pe.kipu.core.domain.usecase.ObserveHomeInsightsUseCase
import pe.kipu.core.domain.usecase.ObserveMonthlyServiceReceiptsUseCase
import pe.kipu.core.domain.usecase.PrepareUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.RegisterUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.UpdateDailyAvailableWidgetUseCase
import pe.kipu.core.domain.voice.LocalVoiceIntentAnalyzer
import pe.kipu.core.domain.voice.VoiceFinancialIntent
import pe.kipu.core.domain.widget.DailyAvailableWidgetGateway
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMovementRepository : MovementRepository {
        val savedMovements = mutableListOf<Movement>()
        var saveResult: Result<Unit> = Result.success(Unit)
        var saveCalls: Int = 0
        private val flow = MutableStateFlow<List<Movement>>(emptyList())

        override fun observeMovements(): Flow<List<Movement>> = flow
        override suspend fun getById(id: EntityId): Movement? = savedMovements.find { it.id == id }
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> {
            saveCalls++
            if (saveResult.isFailure) return saveResult
            savedMovements.add(movement)
            flow.value = savedMovements.toList()
            return saveResult
        }
        override suspend fun delete(id: EntityId): Result<Unit> {
            savedMovements.removeIf { it.id == id }
            flow.value = savedMovements.toList()
            return Result.success(Unit)
        }
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getById(id: EntityId): Category? = null
        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeCommitmentRepository : CommitmentRepository {
        override fun observeCommitments(): Flow<List<Commitment>> = flowOf(emptyList())
        override suspend fun getById(id: EntityId): Commitment? = null
        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeEnvelopeRepository : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(emptyList())
        override suspend fun getById(id: EntityId): Envelope? = null
        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeFinancialPlanRepository : FinancialPlanRepository {
        val planFlow = MutableStateFlow<List<FinancialPlan>>(emptyList())
        override fun observePlans(): Flow<List<FinancialPlan>> = planFlow
        override suspend fun getById(id: EntityId): FinancialPlan? = planFlow.value.find { it.id == id }
        override suspend fun save(plan: FinancialPlan): Result<Unit> {
            planFlow.value = listOf(plan)
            return Result.success(Unit)
        }
        override suspend fun delete(id: EntityId): Result<Unit> {
            planFlow.value = emptyList()
            return Result.success(Unit)
        }
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

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val prefs = MutableStateFlow(UserPreferences())
        override fun observePreferences(): Flow<UserPreferences> = prefs
        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit> {
            prefs.value = transform(prefs.value)
            return Result.success(Unit)
        }
        override suspend fun clear(): Result<Unit> {
            prefs.value = UserPreferences()
            return Result.success(Unit)
        }
    }

    private class FakeGatheringExpenseRepository : GatheringExpenseRepository {
        override fun observeTotalsByGathering(): Flow<Map<EntityId, Money>> = flowOf(emptyMap())
        override fun observeExpensesByGathering(): Flow<Map<EntityId, List<GatheringExpense>>> = flowOf(emptyMap())
        override fun observeLinkedMovementIds(): Flow<Set<EntityId>> = flowOf(emptySet())
        override fun observeActiveGatheringLinkedMovementIds(): Flow<Set<EntityId>> = flowOf(emptySet())
        override suspend fun isMovementLinked(movementId: EntityId): Boolean = false
        override suspend fun save(expense: GatheringExpense): Result<Unit> = Result.success(Unit)
    }

    private inner class FakeMonthlyServiceReceiptRepository : MonthlyServiceReceiptRepository {
        private val receipts = mutableMapOf<String, MonthlyServiceReceipt>()
        override fun observeReceiptsForMonth(monthKey: String): Flow<List<MonthlyServiceReceipt>> =
            flowOf(receipts.values.filter { it.monthKey == monthKey })
        override fun observeAllPaidMovementIds(): Flow<Set<EntityId>> =
            flowOf(receipts.values.mapNotNull { it.paidMovementId }.toSet())
        override suspend fun saveReceipt(receipt: MonthlyServiceReceipt) {
            receipts["${receipt.monthKey}-${receipt.key.identifier}"] = receipt
        }
        override suspend fun getReceipt(monthKey: String, serviceKeyIdentifier: String): MonthlyServiceReceipt? =
            receipts["$monthKey-$serviceKeyIdentifier"]

        override suspend fun markPaid(
            receipt: MonthlyServiceReceipt,
            movement: Movement,
            auditEntry: pe.kipu.core.domain.model.MovementAuditEntry,
        ): Result<Unit> {
            fakeMovementRepository.save(movement).getOrThrow()
            saveReceipt(receipt)
            return Result.success(Unit)
        }

        override suspend fun unmarkPaid(
            receipt: MonthlyServiceReceipt,
            movementId: String?,
            auditEntry: pe.kipu.core.domain.model.MovementAuditEntry?,
        ): Result<Unit> {
            movementId?.let { fakeMovementRepository.delete(it).getOrThrow() }
            saveReceipt(receipt)
            return Result.success(Unit)
        }
    }

    private class FakeDailyAvailableWidgetGateway : DailyAvailableWidgetGateway {
        override suspend fun requestRefresh() {}
    }

    private val fakeMovementRepository = FakeMovementRepository()
    private val fakeCategoryRepository = FakeCategoryRepository()
    private val fakeCommitmentRepository = FakeCommitmentRepository()
    private val fakeEnvelopeRepository = FakeEnvelopeRepository()
    private val fakeFinancialPlanRepository = FakeFinancialPlanRepository()
    private val fakeReserveEventRepository = FakeReserveEventRepository()
    private val fakeUserPreferencesRepository = FakeUserPreferencesRepository()
    private val fakeGatheringExpenseRepository = FakeGatheringExpenseRepository()
    private val fakeMonthlyServiceReceiptRepository = FakeMonthlyServiceReceiptRepository()
    private val fakeWidgetGateway = FakeDailyAvailableWidgetGateway()

    private val fixedTime = Instant.parse("2026-08-16T12:00:00Z")
    private val timeProvider = TimeProvider { fixedTime }

    private val localAnalyzer = LocalVoiceIntentAnalyzer()
    private val analyzeVoiceIntent = AnalyzeVoiceIntentUseCase(localAnalyzer)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        val cycleRangeCalculator = CycleRangeCalculator(timeProvider)
        val observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = fakeEnvelopeRepository,
            movementRepository = fakeMovementRepository,
            gatheringExpenseRepository = fakeGatheringExpenseRepository,
            monthlyServiceReceiptRepository = fakeMonthlyServiceReceiptRepository,
            financialPlanRepository = fakeFinancialPlanRepository,
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                calculateCategoryPeriodSpent = CalculateCategoryPeriodSpentUseCase(),
            ),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val observeInsights = ObserveHomeInsightsUseCase(
            observeEnvelopeBudgets = observeEnvelopeBudgets,
            movementRepository = fakeMovementRepository,
            commitmentRepository = fakeCommitmentRepository,
            categoryRepository = fakeCategoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            financialPlanRepository = fakeFinancialPlanRepository,
            reserveEventRepository = fakeReserveEventRepository,
            calculateCycleAvailable = CalculateCycleAvailableUseCase(
                calculatePeriodEnvelopeTotals = CalculatePeriodEnvelopeTotalsUseCase(),
            ),
            detectAntSpending = DetectAntSpendingUseCase(),
            detectAntSpendingWeeklyLimitUseCase = DetectAntSpendingWeeklyLimitUseCase(),
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
            calculateCategoryExpenseDistribution = pe.kipu.core.domain.usecase.CalculateCategoryExpenseDistributionUseCase(),
            calculateReserveBalance = pe.kipu.core.domain.usecase.CalculateReserveBalanceUseCase(),
            calculateAvailableBalance = pe.kipu.core.domain.usecase.CalculateAvailableBalanceUseCase(),
            cycleRangeCalculator = cycleRangeCalculator,
            timeProvider = timeProvider,
        )

        val updateWidget = UpdateDailyAvailableWidgetUseCase(
            userPreferencesRepository = fakeUserPreferencesRepository,
            widgetGateway = fakeWidgetGateway,
            timeProvider = timeProvider,
        )

        val observeReceipts = ObserveMonthlyServiceReceiptsUseCase(
            financialPlanRepository = fakeFinancialPlanRepository,
            monthlyServiceReceiptRepository = fakeMonthlyServiceReceiptRepository,
            movementRepository = fakeMovementRepository,
            timeProvider = timeProvider,
        )

        val fakeMovementAuditRepository = object : pe.kipu.core.domain.repository.MovementAuditRepository {
            override fun observeAuditLogs(): kotlinx.coroutines.flow.Flow<List<pe.kipu.core.domain.model.MovementAuditEntry>> =
                kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun recordAudit(entry: pe.kipu.core.domain.model.MovementAuditEntry): Result<Unit> =
                Result.success(Unit)
            override suspend fun getAll(): List<pe.kipu.core.domain.model.MovementAuditEntry> = emptyList()
        }

        val markPaid = MarkServiceReceiptPaidUseCase(
            monthlyServiceReceiptRepository = fakeMonthlyServiceReceiptRepository,
            timeProvider = timeProvider,
        )

        val unmarkPaid = UnmarkServiceReceiptPaidUseCase(
            monthlyServiceReceiptRepository = fakeMonthlyServiceReceiptRepository,
            movementRepository = fakeMovementRepository,
            timeProvider = timeProvider,
        )

        val createManualMovement = CreateManualMovementUseCase(
            movementRepository = fakeMovementRepository,
            movementAuditRepository = fakeMovementAuditRepository,
            timeProvider = timeProvider,
            reserveEventRepository = fakeReserveEventRepository,
        )

        return HomeViewModel(
            observeHomeInsights = observeInsights,
            categoryRepository = fakeCategoryRepository,
            envelopeRepository = fakeEnvelopeRepository,
            updateDailyAvailableWidget = updateWidget,
            observeMonthlyServiceReceipts = observeReceipts,
            markServiceReceiptPaid = markPaid,
            unmarkServiceReceiptPaid = unmarkPaid,
            createManualMovement = createManualMovement,
            commitmentRepository = fakeCommitmentRepository,
            analyzeVoiceIntent = analyzeVoiceIntent,
            contributeMonthlyReserve = ContributeMonthlyReserveUseCase(fakeReserveEventRepository, timeProvider),
            prepareUnexpectedExpense = PrepareUnexpectedExpenseUseCase(
                observeHomeInsights = observeInsights,
                observeEnvelopeBudgets = observeEnvelopeBudgets,
                calculateCoverage = CalculateUnexpectedExpenseCoverageUseCase(),
                buildRecoveryPlan = BuildUnexpectedExpenseRecoveryPlanUseCase(),
            ),
            registerUnexpectedExpense = RegisterUnexpectedExpenseUseCase(
                createManualMovement = createManualMovement,
                applyRecoveryPlan = ApplyRecoveryPlanUseCase(fakeEnvelopeRepository),
            ),
        )
    }

    @Test
    fun `voice unexpected expense requires coverage confirmation before saving`() = runTest {
        fakeFinancialPlanRepository.planFlow.value = listOf(
            FinancialPlan(
                id = "plan-1",
                estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
                fixedExpenses = Money.ZERO,
                initialBalance = Money.of(BigDecimal("1000.00")).getOrError(),
            ),
        )
        fakeReserveEventRepository.events.value = listOf(
            ReserveEvent(
                id = "reserve-contribution-1",
                type = pe.kipu.core.domain.model.ReserveEventType.CONTRIBUTION,
                amount = Money.of(BigDecimal("100.00")).getOrError(),
                occurredAt = fixedTime,
                createdAt = fixedTime,
            ),
        )
        val viewModel = createViewModel()
        runCurrent()
        val intent = VoiceFinancialIntent.Expense(
            rawText = "Compré un microondas por 300 soles",
            amount = Money.of(BigDecimal("300.00")).getOrError(),
            categoryId = CategoryIds.OTHER,
            description = "Microondas",
        )

        viewModel.saveVoiceIntent(intent, isUnexpectedExpense = true)
        runCurrent()

        assertTrue(fakeMovementRepository.savedMovements.isEmpty())
        val confirmation = viewModel.voiceUnexpectedExpense.value
        assertNotNull(confirmation)
        assertEquals(BigDecimal("100.00"), confirmation?.preview?.coverage?.fromReserve?.amount)

        viewModel.confirmVoiceUnexpectedExpense(applyAdjustments = false)
        runCurrent()

        assertEquals(1, fakeMovementRepository.savedMovements.size)
        assertNull(viewModel.voiceUnexpectedExpense.value)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `contribute reserve records the plan target once`() = runTest {
        val target = Money.of(BigDecimal("200.00")).getOrError()
        fakeFinancialPlanRepository.planFlow.value = listOf(
            FinancialPlan(
                id = "plan-1",
                estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
                fixedExpenses = Money.ZERO,
                initialBalance = Money.of(BigDecimal("1000.00")).getOrError(),
                reserveMonthlyContribution = target,
            ),
        )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.contributeMonthlyReserve()
        runCurrent()
        viewModel.contributeMonthlyReserve()
        runCurrent()

        assertEquals(1, fakeReserveEventRepository.events.value.size)
        assertEquals(target, fakeReserveEventRepository.events.value.single().amount)
        assertFalse(viewModel.isContributingReserve.value)
        assertNull(viewModel.reserveContributionError.value)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `contribute reserve is blocked when available balance is insufficient`() = runTest {
        fakeFinancialPlanRepository.planFlow.value = listOf(
            FinancialPlan(
                id = "plan-1",
                estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
                fixedExpenses = Money.ZERO,
                reserveMonthlyContribution = Money.of(BigDecimal("200.00")).getOrError(),
            ),
        )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.contributeMonthlyReserve()
        runCurrent()

        assertTrue(fakeReserveEventRepository.events.value.isEmpty())
        assertEquals("Tu saldo disponible aún no alcanza para este aporte.", viewModel.reserveContributionError.value)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `emits content with financial plan when plan repository has plan`() = runTest {
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("1800.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("800.00")).getOrError(),
            budgetCycle = pe.kipu.core.domain.model.BudgetCycle.MONTHLY,
        )
        fakeFinancialPlanRepository.planFlow.value = listOf(plan)
        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Content)
        val content = state as HomeUiState.Content
        assertEquals(plan, content.insights.financialPlan)
        assertEquals(pe.kipu.core.domain.model.BudgetCycle.MONTHLY, content.insights.cycleAvailable.cycle)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `onVoiceTranscriptionReceived parses recognized text and updates state`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.parsedVoiceIntent.value)

        viewModel.onVoiceTranscriptionReceived("Gasté 5 soles en comida")
        runCurrent()

        val parsed = viewModel.parsedVoiceIntent.value
        assertNotNull(parsed)
        assertTrue(parsed is VoiceFinancialIntent.Expense)
        val expense = parsed as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.FOOD, expense.categoryId)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `clearParsedVoiceIntent resets parsed state to null`() = runTest {
        val viewModel = createViewModel()

        viewModel.onVoiceTranscriptionReceived("Gasté 5 soles en comida")
        runCurrent()
        assertNotNull(viewModel.parsedVoiceIntent.value)

        viewModel.clearParsedVoiceIntent()
        assertNull(viewModel.parsedVoiceIntent.value)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `saveVoiceIntent persists expense movement to repository`() = runTest {
        val viewModel = createViewModel()
        val intent = VoiceFinancialIntent.Expense(
            rawText = "Gasté 5 soles en comida",
            amount = Money.of(BigDecimal("5.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            description = "Comida",
            channel = PaymentChannel.CASH,
        )

        viewModel.saveVoiceIntent(intent)
        runCurrent()

        assertEquals(1, fakeMovementRepository.savedMovements.size)
        val saved = fakeMovementRepository.savedMovements.first()
        assertEquals(MovementType.EXPENSE, saved.type)
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), saved.amount)
        assertEquals(CategoryIds.FOOD, saved.categoryId)
        assertEquals(MovementStatus.CONFIRMED, saved.status)
        assertEquals("Comida", saved.description)
        assertEquals(fixedTime, saved.recordedAt)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `voice service payment saves actual amount instead of plan reference`() = runTest {
        val reference = Money.of(BigDecimal("45.00")).getOrError()
        fakeFinancialPlanRepository.planFlow.value = listOf(
            FinancialPlan(
                id = "plan-1",
                estimatedMonthlyIncome = Money.of(BigDecimal("2000.00")).getOrError(),
                fixedExpenses = reference,
                electricityExpenses = reference,
            ),
        )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onVoiceTranscriptionReceived("He pagado 55 soles del recibo de luz")
        runCurrent()
        viewModel.saveVoiceIntent(requireNotNull(viewModel.parsedVoiceIntent.value))
        runCurrent()

        assertEquals(Money.of(BigDecimal("55.00")).getOrError(), fakeMovementRepository.savedMovements.single().amount)
        assertTrue(requireNotNull(fakeMonthlyServiceReceiptRepository.getReceipt("2026-08", "LIGHT")).isPaid)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `saveVoiceIntent blocks duplicate submit and keeps confirmation open when persistence fails`() = runTest {
        fakeMovementRepository.saveResult = Result.failure(IllegalStateException("storage unavailable"))
        val viewModel = createViewModel()
        var saved = false
        val intent = VoiceFinancialIntent.Expense(
            rawText = "Gasté 5 soles en comida",
            amount = Money.of(BigDecimal("5.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            description = "Comida",
            channel = PaymentChannel.CASH,
        )

        viewModel.saveVoiceIntent(intent) { saved = true }
        viewModel.saveVoiceIntent(intent) { saved = true }
        runCurrent()

        assertFalse(saved)
        assertFalse(viewModel.isSavingVoice.value)
        assertNotNull(viewModel.voiceSaveError.value)
        assertEquals(1, fakeMovementRepository.saveCalls)
        assertEquals(0, fakeMovementRepository.savedMovements.size)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `markReceiptPaid then unmarkReceiptPaid toggles receipt and removes movement`() = runTest {
        val viewModel = createViewModel()
        val receipt = MonthlyServiceReceipt(
            key = pe.kipu.core.domain.receipt.ServiceReceiptKey.LIGHT,
            title = "Luz",
            configuredAmount = Money.of(BigDecimal("60.00")).getOrError(),
            monthKey = "2026-08",
            isPaid = false,
        )

        viewModel.markReceiptPaid(receipt, receipt.configuredAmount)
        runCurrent()

        assertEquals(1, fakeMovementRepository.savedMovements.size)
        val paidReceipt = fakeMonthlyServiceReceiptRepository.getReceipt("2026-08", "LIGHT")
        assertNotNull(paidReceipt)
        assertTrue(paidReceipt!!.isPaid)

        viewModel.unmarkReceiptPaid(paidReceipt)
        runCurrent()

        val unmarkedReceipt = fakeMonthlyServiceReceiptRepository.getReceipt("2026-08", "LIGHT")
        assertNotNull(unmarkedReceipt)
        assertFalse(unmarkedReceipt!!.isPaid)
        assertEquals(0, fakeMovementRepository.savedMovements.size)

        viewModel.viewModelScope.cancel()
    }
}
