package pe.kipu.core.domain.plan

interface PlanSetupRepository {
    suspend fun save(setup: PlanSetup): Result<Unit>
}
