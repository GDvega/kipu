package pe.kipu.feature.movements.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.DuplicateDismissalRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.usecase.ConfirmPendingNotificationMovementUseCase
import pe.kipu.core.domain.usecase.CreateManualMovementUseCase
import pe.kipu.core.domain.usecase.DeleteMovementUseCase
import pe.kipu.core.domain.usecase.DetectDuplicateMovementUseCase
import pe.kipu.core.domain.usecase.DismissDuplicatePairUseCase
import pe.kipu.core.domain.usecase.DismissPendingNotificationMovementUseCase
import pe.kipu.core.domain.usecase.FindMovementDuplicatePairsUseCase
import pe.kipu.core.domain.usecase.LinkMovementToCommitmentUseCase
import pe.kipu.core.domain.usecase.ObserveMovementAuditLogsUseCase
import pe.kipu.core.domain.usecase.ObserveMovementDuplicatePairsUseCase
import pe.kipu.core.domain.usecase.ObservePendingNotificationMovementsUseCase
import pe.kipu.core.domain.usecase.ObserveSavingsGoalCommitmentsUseCase
import pe.kipu.core.domain.usecase.ResolveDuplicateMovementUseCase
import pe.kipu.core.domain.usecase.UpdateMovementCategoryUseCase
import pe.kipu.core.domain.usecase.UpdateMovementUseCase
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MovementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeMovementRepo = FakeMovementRepo()
    private val fakeCategoryRepo = FakeCategoryRepo()
    private val fakeEnvelopeRepo = FakeEnvelopeRepo()
    private val fakeAuditRepo = FakeMovementAuditRepo()
    private val fakeCommitmentRepo = FakeCommitmentRepo()
    private val fakeDismissalRepo = FakeDuplicateDismissalRepo()

    private val timeProvider = object : pe.kipu.core.domain.time.TimeProvider {
        override fun now(): Instant = Instant.parse("2026-08-22T12:00:00Z")
    }

    private lateinit var viewModel: MovementsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val matcher = MovementDuplicateMatcher()
        val detectDuplicateMovement = DetectDuplicateMovementUseCase(matcher)
        val findDuplicatePairs = FindMovementDuplicatePairsUseCase(matcher)

        viewModel = MovementsViewModel(
            movementRepository = fakeMovementRepo,
            categoryRepository = fakeCategoryRepo,
            envelopeRepository = fakeEnvelopeRepo,
            observePendingNotificationMovements = ObservePendingNotificationMovementsUseCase(fakeMovementRepo),
            observeMovementDuplicatePairs = ObserveMovementDuplicatePairsUseCase(fakeMovementRepo, fakeDismissalRepo, findDuplicatePairs),
            observeSavingsGoalCommitments = ObserveSavingsGoalCommitmentsUseCase(fakeCommitmentRepo),
            observeMovementAuditLogs = ObserveMovementAuditLogsUseCase(fakeAuditRepo, fakeCategoryRepo),
            resolveDuplicateMovement = ResolveDuplicateMovementUseCase(fakeMovementRepo),
            dismissDuplicatePair = DismissDuplicatePairUseCase(fakeDismissalRepo),
            confirmPendingNotificationMovement = ConfirmPendingNotificationMovementUseCase(fakeMovementRepo, detectDuplicateMovement, fakeAuditRepo),
            dismissPendingNotificationMovement = DismissPendingNotificationMovementUseCase(fakeMovementRepo),
            updateMovementCategory = UpdateMovementCategoryUseCase(fakeMovementRepo, fakeCategoryRepo),
            linkMovementToCommitment = LinkMovementToCommitmentUseCase(fakeMovementRepo, fakeCommitmentRepo),
            createManualMovement = CreateManualMovementUseCase(
                fakeMovementRepo,
                fakeAuditRepo,
                timeProvider,
                FakeReserveEventRepo(),
            ),
            updateMovement = UpdateMovementUseCase(
                fakeMovementRepo,
                fakeCategoryRepo,
                fakeAuditRepo,
                FakeReserveEventRepo(),
                timeProvider,
            ),
            deleteMovement = DeleteMovementUseCase(
                fakeMovementRepo,
                fakeAuditRepo,
                FakeReserveEventRepo(),
                timeProvider,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads audit logs and allows tab and filter switching`() = runTest(testDispatcher) {
        val entry1 = MovementAuditEntry(
            id = "audit-1",
            movementId = "mov-1",
            action = MovementAuditAction.CREATED,
            movementType = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("30.00")).getOrError(),
            categoryId = "cat-food",
            channel = PaymentChannel.YAPE,
            timestamp = Instant.now(),
        )
        val entry2 = MovementAuditEntry(
            id = "audit-2",
            movementId = "mov-1",
            action = MovementAuditAction.DELETED,
            movementType = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("30.00")).getOrError(),
            categoryId = "cat-food",
            channel = PaymentChannel.YAPE,
            timestamp = Instant.now(),
        )
        fakeAuditRepo.setLogs(listOf(entry1, entry2))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MovementsUiState.Content)
        val content = state as MovementsUiState.Content
        assertEquals(MovementsTab.ACTIVE, content.selectedTab)
        assertEquals(2, content.auditLogs.size)

        // Switch to AUDIT tab
        viewModel.onTabSelected(MovementsTab.AUDIT)
        advanceUntilIdle()

        val auditState = viewModel.uiState.value as MovementsUiState.Content
        assertEquals(MovementsTab.AUDIT, auditState.selectedTab)
        assertEquals(2, auditState.filteredAuditLogs.size)

        // Filter by DELETED
        viewModel.onAuditFilterSelected(MovementAuditFilter.DELETED)
        advanceUntilIdle()

        val deletedFilterState = viewModel.uiState.value as MovementsUiState.Content
        assertEquals(MovementAuditFilter.DELETED, deletedFilterState.selectedAuditFilter)
        assertEquals(1, deletedFilterState.filteredAuditLogs.size)
        assertEquals(MovementAuditAction.DELETED, deletedFilterState.filteredAuditLogs[0].action)
    }

    @Test
    fun `selecting an envelope assigns its identity and category to the manual expense`() = runTest(testDispatcher) {
        fakeEnvelopeRepo.envelopesFlow.value = listOf(
            Envelope(
                id = "envelope-leisure",
                name = "Ocio",
                weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
                categoryId = "category-other",
            ),
        )
        advanceUntilIdle()

        viewModel.onAddMovementClick()
        viewModel.onManualEnvelopeSelected("envelope-leisure")
        advanceUntilIdle()

        val form = (viewModel.uiState.value as MovementsUiState.Content).manualMovementForm
        assertEquals("envelope-leisure", form?.envelopeId)
        assertEquals("category-other", form?.categoryId)
    }

    private class FakeMovementRepo : MovementRepository {
        val movementsFlow = MutableStateFlow<List<Movement>>(emptyList())
        override fun observeMovements(): Flow<List<Movement>> = movementsFlow
        override suspend fun getById(id: EntityId): Movement? = movementsFlow.value.find { it.id == id }
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> {
            movementsFlow.value = listOf(movement) + movementsFlow.value.filter { it.id != movement.id }
            return Result.success(Unit)
        }
        override suspend fun delete(id: EntityId): Result<Unit> {
            movementsFlow.value = movementsFlow.value.filter { it.id != id }
            return Result.success(Unit)
        }
    }

    private class FakeCategoryRepo : CategoryRepository {
        val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
        override fun observeCategories(): Flow<List<Category>> = categoriesFlow
        override suspend fun getById(id: EntityId): Category? = categoriesFlow.value.find { it.id == id }
        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeEnvelopeRepo : EnvelopeRepository {
        val envelopesFlow = MutableStateFlow<List<Envelope>>(emptyList())
        override fun observeEnvelopes(): Flow<List<Envelope>> = envelopesFlow
        override suspend fun getById(id: EntityId): Envelope? = envelopesFlow.value.find { it.id == id }
        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeMovementAuditRepo : MovementAuditRepository {
        val logsFlow = MutableStateFlow<List<MovementAuditEntry>>(emptyList())
        fun setLogs(logs: List<MovementAuditEntry>) { logsFlow.value = logs }
        override fun observeAuditLogs(): Flow<List<MovementAuditEntry>> = logsFlow
        override suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit> {
            logsFlow.value = listOf(entry) + logsFlow.value
            return Result.success(Unit)
        }
        override suspend fun getAll(): List<MovementAuditEntry> = logsFlow.value
    }

    private class FakeCommitmentRepo : CommitmentRepository {
        override fun observeCommitments(): Flow<List<Commitment>> = flowOf(emptyList())
        override suspend fun getById(id: EntityId): Commitment? = null
        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }

    private class FakeDuplicateDismissalRepo : DuplicateDismissalRepository {
        val dismissedFlow = MutableStateFlow<Set<String>>(emptySet())
        override fun observeDismissedPairKeys(): Flow<Set<String>> = dismissedFlow
        override suspend fun dismiss(pairKey: String): Result<Unit> {
            dismissedFlow.value = dismissedFlow.value + pairKey
            return Result.success(Unit)
        }
    }

    private class FakeReserveEventRepo : ReserveEventRepository {
        override fun observeAll(): Flow<List<ReserveEvent>> = flowOf(emptyList())
        override suspend fun getById(id: String): ReserveEvent? = null
        override suspend fun record(event: ReserveEvent): Result<Unit> = Result.success(Unit)
    }
}
