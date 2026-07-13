package pe.kipu.core.domain.plan

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan

/**
 * Immutable source of truth produced by the plan wizard before persistence.
 * Every model already has its definitive identity and can be validated and saved unchanged.
 */
data class PlanSetup(
    val plan: FinancialPlan,
    val categories: List<Category>,
    val envelopes: List<Envelope>,
    val commitmentsToSave: List<Commitment>,
    val commitmentIdsToSettle: Set<EntityId>,
)
