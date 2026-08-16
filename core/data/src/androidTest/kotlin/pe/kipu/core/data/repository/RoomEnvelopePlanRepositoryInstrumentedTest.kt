package pe.kipu.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.EnvelopePlanRepository

@RunWith(AndroidJUnit4::class)
class RoomEnvelopePlanRepositoryInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: KipuDatabase
    private lateinit var repository: EnvelopePlanRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, KipuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomEnvelopePlanRepository(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun createFailureRollsBackPlanLink() = runBlocking {
        database.financialPlanDao().upsert(plan(emptyList()).toEntity())
        val updatedPlan = plan(listOf(ENVELOPE_ID))
        val envelopeWithMissingCategory = envelope(categoryId = "missing-category")

        val result = repository.saveEnvelopeWithPlan(envelopeWithMissingCategory, updatedPlan)

        assertTrue(result.isFailure)
        assertNull(database.envelopeDao().getById(ENVELOPE_ID))
        assertEquals(
            "",
            database.financialPlanDao().getById(FinancialPlanIds.PRIMARY)?.envelopeIds,
        )
    }

    @Test
    fun deleteFailureRollsBackPlanUnlink() = runBlocking {
        seedLinkedEnvelope()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER prevent_envelope_delete
            BEFORE DELETE ON envelopes
            BEGIN
                SELECT RAISE(ABORT, 'forced delete failure');
            END
            """.trimIndent(),
        )

        val result = repository.deleteEnvelopeWithPlan(ENVELOPE_ID, plan(emptyList()))

        assertTrue(result.isFailure)
        assertNotNull(database.envelopeDao().getById(ENVELOPE_ID))
        assertEquals(
            ENVELOPE_ID,
            database.financialPlanDao().getById(FinancialPlanIds.PRIMARY)?.envelopeIds,
        )
    }

    @Test
    fun successfulCreateAndDeleteKeepEnvelopeAndPlanCoherent() = runBlocking {
        database.categoryDao().upsert(Category(CATEGORY_ID, "Comida").toEntity())
        database.financialPlanDao().upsert(plan(emptyList()).toEntity())

        val createResult = repository.saveEnvelopeWithPlan(
            envelope(CATEGORY_ID),
            plan(listOf(ENVELOPE_ID)),
        )

        assertTrue(createResult.isSuccess)
        assertNotNull(database.envelopeDao().getById(ENVELOPE_ID))
        assertEquals(
            ENVELOPE_ID,
            database.financialPlanDao().getById(FinancialPlanIds.PRIMARY)?.envelopeIds,
        )

        val deleteResult = repository.deleteEnvelopeWithPlan(ENVELOPE_ID, plan(emptyList()))

        assertTrue(deleteResult.isSuccess)
        assertNull(database.envelopeDao().getById(ENVELOPE_ID))
        assertEquals(
            "",
            database.financialPlanDao().getById(FinancialPlanIds.PRIMARY)?.envelopeIds,
        )
    }

    private suspend fun seedLinkedEnvelope() {
        database.categoryDao().upsert(Category(CATEGORY_ID, "Comida").toEntity())
        database.envelopeDao().upsert(envelope(CATEGORY_ID).toEntity())
        database.financialPlanDao().upsert(plan(listOf(ENVELOPE_ID)).toEntity())
    }

    private fun envelope(categoryId: String) = Envelope(
        id = ENVELOPE_ID,
        name = "Comida",
        weeklyLimit = money("100.00"),
        categoryId = categoryId,
    )

    private fun plan(envelopeIds: List<String>) = FinancialPlan(
        id = FinancialPlanIds.PRIMARY,
        estimatedMonthlyIncome = money("3000.00"),
        fixedExpenses = money("1000.00"),
        envelopeIds = envelopeIds,
    )

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()

    private companion object {
        const val CATEGORY_ID = "category-food"
        const val ENVELOPE_ID = "envelope-food"
    }
}
