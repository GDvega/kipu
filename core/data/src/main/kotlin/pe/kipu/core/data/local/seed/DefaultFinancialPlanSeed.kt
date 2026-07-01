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
        initialBalanceCents = 0L,
        envelopeIds = envelopeIds,
        incomeProfile = "FIXED",
        payFrequency = "MONTHLY",
    )

    fun insertInto(db: SupportSQLiteDatabase) {
        // No-op: a real financial plan must only exist after explicit user confirmation in the wizard.
    }
}
