package pe.kipu.core.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.domain.plan.FinancialPlanIds

object DefaultFinancialPlanSeed {
    private val envelopeIds = listOf(
        DefaultEnvelopeIds.FOOD,
        DefaultEnvelopeIds.TRANSPORT,
        DefaultEnvelopeIds.LEISURE,
        DefaultEnvelopeIds.FAMILY,
        DefaultEnvelopeIds.ANT_SPENDING,
    ).joinToString(",")

    val plan: FinancialPlanEntity = FinancialPlanEntity(
        id = FinancialPlanIds.PRIMARY,
        estimatedMonthlyIncomeCents = 300_000L,
        fixedExpensesCents = 180_000L,
        envelopeIds = envelopeIds,
    )

    fun insertInto(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT OR IGNORE INTO financial_plans (
                id, estimatedMonthlyIncomeCents, fixedExpensesCents, envelopeIds
            ) VALUES (?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(
                plan.id,
                plan.estimatedMonthlyIncomeCents,
                plan.fixedExpensesCents,
                plan.envelopeIds,
            ),
        )
    }
}
