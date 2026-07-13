package pe.kipu.core.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.plan.PlanSetup
import pe.kipu.core.domain.plan.PlanSetupRepository

@Singleton
class RoomPlanSetupRepository @Inject constructor(
    private val database: KipuDatabase,
) : PlanSetupRepository {
    override suspend fun save(setup: PlanSetup): Result<Unit> = executePlanSetupSave(setup) { validatedSetup ->
        database.withTransaction {
            validateDatabaseReferences(validatedSetup)
            validatedSetup.categories.forEach { database.categoryDao().upsert(it.toEntity()) }
            validatedSetup.envelopes.forEach { database.envelopeDao().upsert(it.toEntity()) }
            validatedSetup.commitmentsToSave.forEach { database.commitmentDao().upsert(it.toEntity()) }
            if (validatedSetup.commitmentIdsToSettle.isNotEmpty()) {
                database.commitmentDao().settleByIds(validatedSetup.commitmentIdsToSettle)
            }
            database.financialPlanDao().upsert(validatedSetup.plan.toEntity())
        }
    }

    private suspend fun validateDatabaseReferences(setup: PlanSetup) {
        val categoriesIncludedInSetup = setup.categories.mapTo(mutableSetOf()) { it.id }
        val externalCategoryIds = setup.envelopes
            .mapTo(mutableSetOf()) { it.categoryId }
            .minus(categoriesIncludedInSetup)
        if (externalCategoryIds.isNotEmpty()) {
            val existingCategoryIds = database.categoryDao().getExistingIds(externalCategoryIds).toSet()
            require(existingCategoryIds == externalCategoryIds) {
                "Plan setup references a category that does not exist"
            }
        }

        val idsToSettle = setup.commitmentIdsToSettle
        if (idsToSettle.isNotEmpty()) {
            val existingCommitmentIds = database.commitmentDao().getExistingIds(idsToSettle).toSet()
            require(existingCommitmentIds == idsToSettle) {
                "Plan setup requests settlement for a commitment that does not exist"
            }
        }
    }
}
