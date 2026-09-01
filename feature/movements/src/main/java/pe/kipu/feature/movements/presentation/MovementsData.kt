package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementDuplicatePair

internal data class MovementsData(
    val movements: List<Movement>,
    val categories: List<Category>,
    val envelopes: List<Envelope> = emptyList(),
    val pendingNotificationIncomes: List<Movement>,
    val duplicatePairs: List<MovementDuplicatePair>,
    val selectedFilter: MovementChannelFilter,
    val savingsGoals: List<Commitment> = emptyList(),
    val auditLogs: List<MovementAuditEntry> = emptyList(),
)
