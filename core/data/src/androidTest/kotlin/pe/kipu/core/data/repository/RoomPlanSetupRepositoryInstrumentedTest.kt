package pe.kipu.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.entity.CategoryEntity
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.data.local.entity.GatheringEntity
import pe.kipu.core.data.local.entity.MovementEntity
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.PlanSetup

@RunWith(AndroidJUnit4::class)
class RoomPlanSetupRepositoryInstrumentedTest {
    private lateinit var database: KipuDatabase
    private lateinit var repository: RoomPlanSetupRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPlanSetupRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesCompleteSetupWithExactIdsAndRelationships() = runBlocking {
        val setup = completeSetup()

        val result = repository.save(setup)

        assertTrue(result.isSuccess)
        val category = database.categoryDao().getById(CATEGORY_NEW)
        val envelope = database.envelopeDao().getById(ENVELOPE_NEW)
        val commitment = database.commitmentDao().getById(COMMITMENT_NEW)
        val plan = database.financialPlanDao().getById(PLAN_PRIMARY)
        assertEquals(CATEGORY_NEW, category?.id)
        assertEquals(ENVELOPE_NEW, envelope?.id)
        assertEquals(CATEGORY_NEW, envelope?.categoryId)
        assertEquals(COMMITMENT_NEW, commitment?.id)
        assertEquals(ENVELOPE_NEW, plan?.envelopeIds)
    }

    @Test
    fun savesEnvelopeThatReferencesCategoryAlreadyInDatabase() = runBlocking {
        database.categoryDao().upsert(CategoryEntity(CATEGORY_EXISTING, "Existente", null))
        val existingCategoryEnvelope = envelope(categoryId = CATEGORY_EXISTING)
        val setup = completeSetup(
            category = Category(CATEGORY_EXISTING, "Existente"),
            envelope = existingCategoryEnvelope,
        ).copy(categories = emptyList())

        val result = repository.save(setup)

        assertTrue(result.isSuccess)
        assertEquals(
            CATEGORY_EXISTING,
            database.envelopeDao().getById(ENVELOPE_NEW)?.categoryId,
        )
    }

    @Test
    fun updatesExistingCommitmentWithoutCreatingAnotherIdentity() = runBlocking {
        database.commitmentDao().upsert(commitmentEntity(COMMITMENT_NEW, title = "Anterior"))

        val result = repository.save(completeSetup())

        assertTrue(result.isSuccess)
        assertEquals("Meta nueva", database.commitmentDao().getById(COMMITMENT_NEW)?.title)
        assertEquals(1, countRows("commitments"))
    }

    @Test
    fun settlesExistingCommitmentAndRepeatedSettlementIsIdempotent() = runBlocking {
        database.commitmentDao().upsert(commitmentEntity(COMMITMENT_SETTLE, isSettled = false))
        val setup = completeSetup().copy(
            commitmentsToSave = emptyList(),
            commitmentIdsToSettle = setOf(COMMITMENT_SETTLE),
        )

        val first = repository.save(setup)
        val second = repository.save(setup)

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertTrue(database.commitmentDao().getById(COMMITMENT_SETTLE)?.isSettled == true)
        assertEquals(1, countRows("commitments"))
    }

    @Test
    fun failsAndRollsBackWhenSettlementIdentityDoesNotExist() = runBlocking {
        val setup = completeSetup().copy(
            commitmentsToSave = emptyList(),
            commitmentIdsToSettle = setOf("commitment-missing"),
        )

        val result = repository.save(setup)

        assertTrue(result.isFailure)
        assertNull(database.categoryDao().getById(CATEGORY_NEW))
        assertNull(database.envelopeDao().getById(ENVELOPE_NEW))
        assertNull(database.financialPlanDao().getById(PLAN_PRIMARY))
    }

    @Test
    fun failsAndRollsBackWhenReferencedCategoryDoesNotExist() = runBlocking {
        val setup = completeSetup().copy(categories = emptyList())

        val result = repository.save(setup)

        assertTrue(result.isFailure)
        assertNull(database.envelopeDao().getById(ENVELOPE_NEW))
        assertNull(database.commitmentDao().getById(COMMITMENT_NEW))
        assertNull(database.financialPlanDao().getById(PLAN_PRIMARY))
    }

    @Test
    fun reactivatesCommitmentWithSameIdentity() = runBlocking {
        database.commitmentDao().upsert(commitmentEntity(COMMITMENT_NEW, isSettled = true))

        val result = repository.save(completeSetup())

        assertTrue(result.isSuccess)
        assertFalse(database.commitmentDao().getById(COMMITMENT_NEW)?.isSettled ?: true)
        assertEquals(1, countRows("commitments"))
    }

    @Test
    fun savingSameSetupTwiceKeepsStableCountsAndIdentities() = runBlocking {
        val setup = completeSetup()

        assertTrue(repository.save(setup).isSuccess)
        assertTrue(repository.save(setup).isSuccess)

        assertEquals(1, countRows("categories"))
        assertEquals(1, countRows("envelopes"))
        assertEquals(1, countRows("commitments"))
        assertEquals(1, countRows("financial_plans"))
    }

    @Test
    fun preservesHistoricalMovementsAndUnrelatedGatherings() = runBlocking {
        database.categoryDao().upsert(CategoryEntity(CATEGORY_EXISTING, "Anterior", null))
        database.commitmentDao().upsert(commitmentEntity(COMMITMENT_NEW, title = "Anterior"))
        database.movementDao().upsert(movementEntity())
        database.gatheringDao().upsert(gatheringEntity())
        val setup = completeSetup(
            category = Category(CATEGORY_EXISTING, "Actualizada"),
            envelope = envelope(categoryId = CATEGORY_EXISTING),
        )

        val result = repository.save(setup)

        assertTrue(result.isSuccess)
        assertNotNull(database.movementDao().getById(MOVEMENT_HISTORY))
        assertEquals(COMMITMENT_NEW, database.movementDao().getById(MOVEMENT_HISTORY)?.commitmentId)
        assertNotNull(database.gatheringDao().getById(GATHERING_UNRELATED))
    }

    @Test
    fun rollsBackWhenCategoryWriteFails() = runBlocking {
        assertRollbackAt(RollbackStage.CATEGORY)
    }

    @Test
    fun rollsBackWhenEnvelopeWriteFailsAfterCategory() = runBlocking {
        assertRollbackAt(RollbackStage.ENVELOPE)
    }

    @Test
    fun rollsBackWhenCommitmentWriteFailsAfterCategoryAndEnvelope() = runBlocking {
        assertRollbackAt(RollbackStage.COMMITMENT)
    }

    @Test
    fun rollsBackWhenSettlementFailsAfterPreviousWrites() = runBlocking {
        assertRollbackAt(RollbackStage.SETTLEMENT)
    }

    @Test
    fun rollsBackWhenPlanWriteFailsAtEnd() = runBlocking {
        assertRollbackAt(RollbackStage.PLAN)
    }

    private suspend fun assertRollbackAt(stage: RollbackStage) {
        seedRollbackBaseline()
        installFailureTrigger(stage)

        val result = repository.save(
            completeSetup().copy(commitmentIdsToSettle = setOf(COMMITMENT_SETTLE)),
        )

        assertTrue("Expected failure at $stage", result.isFailure)
        assertNull(database.categoryDao().getById(CATEGORY_NEW))
        assertNull(database.envelopeDao().getById(ENVELOPE_NEW))
        assertNull(database.commitmentDao().getById(COMMITMENT_NEW))
        assertFalse(database.commitmentDao().getById(COMMITMENT_SETTLE)?.isSettled ?: true)
        assertEquals(100_000L, database.financialPlanDao().getById(PLAN_PRIMARY)?.estimatedMonthlyIncomeCents)
        assertNotNull(database.movementDao().getById(MOVEMENT_HISTORY))
        assertNotNull(database.gatheringDao().getById(GATHERING_UNRELATED))
    }

    private suspend fun seedRollbackBaseline() {
        database.categoryDao().upsert(CategoryEntity(CATEGORY_EXISTING, "Existente", null))
        database.commitmentDao().upsert(commitmentEntity(COMMITMENT_SETTLE, isSettled = false))
        database.financialPlanDao().upsert(
            FinancialPlanEntity(
                id = PLAN_PRIMARY,
                estimatedMonthlyIncomeCents = 100_000L,
                fixedExpensesCents = 20_000L,
                envelopeIds = "",
            ),
        )
        database.movementDao().upsert(movementEntity(commitmentId = COMMITMENT_SETTLE))
        database.gatheringDao().upsert(gatheringEntity())
    }

    private fun installFailureTrigger(stage: RollbackStage) {
        val sql = when (stage) {
            RollbackStage.CATEGORY -> trigger("INSERT", "categories", "NEW.id = '$CATEGORY_NEW'")
            RollbackStage.ENVELOPE -> trigger("INSERT", "envelopes", "NEW.id = '$ENVELOPE_NEW'")
            RollbackStage.COMMITMENT -> trigger("INSERT", "commitments", "NEW.id = '$COMMITMENT_NEW'")
            RollbackStage.SETTLEMENT -> trigger(
                "UPDATE OF isSettled",
                "commitments",
                "NEW.id = '$COMMITMENT_SETTLE' AND NEW.isSettled = 1",
            )
            RollbackStage.PLAN -> trigger("UPDATE", "financial_plans", "NEW.id = '$PLAN_PRIMARY'")
        }
        database.openHelper.writableDatabase.execSQL(sql)
    }

    private fun trigger(operation: String, table: String, condition: String): String =
        """
        CREATE TRIGGER fail_plan_setup_stage
        BEFORE $operation ON $table
        WHEN $condition
        BEGIN
            SELECT RAISE(ABORT, 'forced plan setup test failure');
        END
        """.trimIndent()

    private fun completeSetup(
        category: Category = Category(CATEGORY_NEW, "Mascota"),
        envelope: Envelope = envelope(),
    ): PlanSetup = PlanSetup(
        plan = FinancialPlan(
            id = PLAN_PRIMARY,
            estimatedMonthlyIncome = money("2000"),
            fixedExpenses = money("500"),
            envelopeIds = listOf(envelope.id),
            budgetCycle = BudgetCycle.WEEKLY,
        ),
        categories = listOf(category),
        envelopes = listOf(envelope),
        commitmentsToSave = listOf(
            Commitment(
                id = COMMITMENT_NEW,
                type = CommitmentType.SAVINGS_GOAL,
                title = "Meta nueva",
                targetAmount = money("1000"),
                isSettled = false,
            ),
        ),
        commitmentIdsToSettle = emptySet(),
    )

    private fun envelope(categoryId: String = CATEGORY_NEW): Envelope = Envelope(
        id = ENVELOPE_NEW,
        name = "Mascota",
        weeklyLimit = money("50"),
        categoryId = categoryId,
    )

    private fun commitmentEntity(
        id: String,
        title: String = "Meta",
        isSettled: Boolean = false,
    ): CommitmentEntity = CommitmentEntity(
        id = id,
        type = CommitmentType.SAVINGS_GOAL.name,
        title = title,
        targetAmountCents = 100_000L,
        currentAmountCents = 10_000L,
        dueDateEpochDay = null,
        counterpartyName = null,
        isSettled = isSettled,
    )

    private fun movementEntity(commitmentId: String = COMMITMENT_NEW): MovementEntity = MovementEntity(
        id = MOVEMENT_HISTORY,
        type = "EXPENSE",
        amountCents = 1_000L,
        categoryId = CATEGORY_EXISTING,
        channel = "CASH",
        source = "MANUAL",
        status = "CONFIRMED",
        description = null,
        counterpartyName = null,
        operationNumber = null,
        commitmentId = commitmentId,
        recordedAtMillis = 1L,
        createdAtMillis = 1L,
    )

    private fun gatheringEntity(): GatheringEntity = GatheringEntity(
        id = GATHERING_UNRELATED,
        name = "Junta",
        participantCount = 1,
        participantNames = "Ana",
    )

    private fun countRows(table: String): Int = database.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM $table")
        .use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun money(value: String): Money = Money.of(value.toBigDecimal()).getOrError()

    private enum class RollbackStage { CATEGORY, ENVELOPE, COMMITMENT, SETTLEMENT, PLAN }

    private companion object {
        const val CATEGORY_NEW = "category-new"
        const val CATEGORY_EXISTING = "category-existing"
        const val ENVELOPE_NEW = "envelope-new"
        const val COMMITMENT_NEW = "commitment-new"
        const val COMMITMENT_SETTLE = "commitment-settle"
        const val PLAN_PRIMARY = "plan-primary"
        const val MOVEMENT_HISTORY = "movement-history"
        const val GATHERING_UNRELATED = "gathering-unrelated"
    }
}
