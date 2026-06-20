package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_plans")
data class FinancialPlanEntity(
    @PrimaryKey val id: String,
    val estimatedMonthlyIncomeCents: Long,
    val fixedExpensesCents: Long,
    /** Comma-separated envelope ids. */
    val envelopeIds: String,
)
