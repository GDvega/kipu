package pe.kipu.core.domain.repository

import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan

/** Persists an envelope mutation and its optional plan-link mutation atomically. */
interface EnvelopePlanRepository {
    suspend fun saveEnvelopeWithPlan(
        envelope: Envelope,
        plan: FinancialPlan?,
    ): Result<Unit>

    suspend fun deleteEnvelopeWithPlan(
        envelopeId: EntityId,
        plan: FinancialPlan?,
    ): Result<Unit>
}
