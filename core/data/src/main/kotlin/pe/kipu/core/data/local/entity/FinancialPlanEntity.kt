package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_plans")
data class FinancialPlanEntity(
    @PrimaryKey val id: String,
    val estimatedMonthlyIncomeCents: Long,
    val fixedExpensesCents: Long,
    val initialBalanceCents: Long = 0L,
    val reserveMonthlyContributionCents: Long = 0L,
    /** Comma-separated envelope ids. */
    val envelopeIds: String,
    val incomeProfile: String = "FIXED",
    val payFrequency: String = "MONTHLY",
    val budgetCycle: String = "WEEKLY",
    val antSpendingLimitCents: Long? = null,
    val antSpendingAlertEnabled: Boolean = true,
    val antSpendingAlertPercent: Int = 80,
    /** Comma-separated category ids. */
    val antSpendingTrackedCategoryIds: String = "",
    val electricityExpensesCents: Long? = null,
    val waterExpensesCents: Long? = null,
    val internetExpensesCents: Long? = null,
    val rentExpensesCents: Long? = null,
    val phoneExpensesCents: Long? = null,
    val debtsExpensesCents: Long? = null,
    val educationExpensesCents: Long? = null,
    val customFixedExpensesJson: String? = null,
)
