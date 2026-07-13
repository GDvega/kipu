package pe.kipu.core.data.repository

import java.util.Locale
import java.util.concurrent.CancellationException
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.plan.PlanSetup

internal suspend fun executePlanSetupSave(
    setup: PlanSetup,
    persist: suspend (PlanSetup) -> Unit,
): Result<Unit> {
    validatePlanSetup(setup)?.let { return Result.failure(it) }
    return try {
        persist(setup)
        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}

private fun validatePlanSetup(setup: PlanSetup): IllegalArgumentException? {
    duplicateId(setup.categories.map { it.id })?.let {
        return invalid("Duplicate category id")
    }
    duplicateId(setup.envelopes.map { it.id })?.let {
        return invalid("Duplicate envelope id")
    }
    duplicateId(setup.commitmentsToSave.map { it.id })?.let {
        return invalid("Duplicate commitment id")
    }
    if (setup.categories.map { it.name.trim().lowercase(Locale.ROOT) }.hasDuplicates()) {
        return invalid("Duplicate category name")
    }
    val commitmentIdsToSave = setup.commitmentsToSave.mapTo(mutableSetOf()) { it.id }
    if (commitmentIdsToSave.any(setup.commitmentIdsToSettle::contains)) {
        return invalid("A commitment cannot be saved and settled together")
    }
    if (setup.commitmentIdsToSettle.any(String::isBlank)) {
        return invalid("Commitment ids to settle must not be blank")
    }
    val envelopeIds = setup.envelopes.map { it.id }
    if (setup.plan.envelopeIds != envelopeIds || setup.plan.envelopeIds.hasDuplicates()) {
        return invalid("Plan envelope ids must match setup envelopes")
    }
    if (setup.plan.validate() is DomainResult.Err ||
        setup.categories.any { it.validate() is DomainResult.Err } ||
        setup.envelopes.any { it.validate() is DomainResult.Err } ||
        setup.commitmentsToSave.any { it.validate() is DomainResult.Err }
    ) {
        return invalid("Plan setup contains an invalid model")
    }
    return null
}

private fun duplicateId(ids: List<String>): String? = ids
    .groupingBy { it }
    .eachCount()
    .entries
    .firstOrNull { it.value > 1 }
    ?.key

private fun <T> List<T>.hasDuplicates(): Boolean = distinct().size != size

private fun invalid(message: String): IllegalArgumentException = IllegalArgumentException(message)
