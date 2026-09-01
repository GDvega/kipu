package pe.kipu.core.domain.export

import java.time.Instant
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.model.UserPreferences

const val USER_DATA_EXPORT_VERSION: Int = 5

data class UserDataSnapshot(
    val exportVersion: Int = USER_DATA_EXPORT_VERSION,
    val exportedAt: Instant,
    val movements: List<Movement>,
    val categories: List<Category>,
    val envelopes: List<Envelope>,
    val commitments: List<Commitment>,
    val financialPlans: List<FinancialPlan>,
    val gatherings: List<Gathering>,
    val gatheringExpenses: List<GatheringExpense>,
    val dismissedDuplicatePairKeys: Set<String>,
    val preferences: UserPreferences,
    val monthlyServiceReceipts: List<MonthlyServiceReceipt> = emptyList(),
    val movementAuditEntries: List<MovementAuditEntry> = emptyList(),
    val reserveEvents: List<ReserveEvent> = emptyList(),
)

data class UserDataExportPayload(
    val content: String,
    val format: ExportFormat,
    val fileName: String,
)
