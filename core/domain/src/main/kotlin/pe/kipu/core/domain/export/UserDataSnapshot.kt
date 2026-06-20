package pe.kipu.core.domain.export

import java.time.Instant
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.UserPreferences

const val USER_DATA_EXPORT_VERSION: Int = 2

data class UserDataSnapshot(
    val exportVersion: Int = USER_DATA_EXPORT_VERSION,
    val exportedAt: Instant,
    val movements: List<Movement>,
    val categories: List<Category>,
    val envelopes: List<Envelope>,
    val commitments: List<Commitment>,
    val financialPlans: List<FinancialPlan>,
    val gatherings: List<Gathering>,
    val dismissedDuplicatePairKeys: Set<String>,
    val preferences: UserPreferences,
)

data class UserDataExportPayload(
    val content: String,
    val format: ExportFormat,
    val fileName: String,
)
