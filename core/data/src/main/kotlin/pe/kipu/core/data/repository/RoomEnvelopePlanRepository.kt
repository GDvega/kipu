package pe.kipu.core.data.repository

import androidx.room.withTransaction
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.repository.EnvelopePlanRepository

@Singleton
class RoomEnvelopePlanRepository @Inject constructor(
    private val database: KipuDatabase,
) : EnvelopePlanRepository {

    override suspend fun saveEnvelopeWithPlan(
        envelope: Envelope,
        plan: FinancialPlan?,
    ): Result<Unit> {
        validateEnvelope(envelope)?.let { return Result.failure(it) }
        validatePlan(plan)?.let { return Result.failure(it) }
        return transactionResult {
            // Plan first lets a later envelope constraint failure prove that both writes roll back.
            plan?.let { database.financialPlanDao().upsert(it.toEntity()) }
            database.envelopeDao().upsert(envelope.toEntity())
        }
    }

    override suspend fun deleteEnvelopeWithPlan(
        envelopeId: EntityId,
        plan: FinancialPlan?,
    ): Result<Unit> {
        validatePlan(plan)?.let { return Result.failure(it) }
        return transactionResult {
            plan?.let { database.financialPlanDao().upsert(it.toEntity()) }
            database.envelopeDao().deleteById(envelopeId)
        }
    }

    private suspend fun transactionResult(block: suspend () -> Unit): Result<Unit> = try {
        database.withTransaction { block() }
        Result.success(Unit)
    } catch (failure: Throwable) {
        if (failure is CancellationException) throw failure
        Result.failure(failure)
    }

    private fun validateEnvelope(envelope: Envelope): IllegalArgumentException? =
        when (val validation = envelope.validate()) {
            is DomainResult.Ok -> null
            is DomainResult.Err -> IllegalArgumentException(validation.error.message)
        }

    private fun validatePlan(plan: FinancialPlan?): IllegalArgumentException? =
        when (val validation = plan?.validate()) {
            null, is DomainResult.Ok -> null
            is DomainResult.Err -> IllegalArgumentException(validation.error.message)
        }
}
