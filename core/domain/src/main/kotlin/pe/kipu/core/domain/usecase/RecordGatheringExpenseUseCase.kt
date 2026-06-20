package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.util.GatheringParticipantValidator
import pe.kipu.core.domain.util.MoneyInputParser

class RecordGatheringExpenseUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(
        gatheringId: EntityId,
        amountInput: String,
        paidByParticipant: String,
        description: String?,
    ): DomainResult<GatheringExpense> {
        val gathering = gatheringRepository.getById(gatheringId)
            ?: return DomainResult.Err(DomainError.NotFound("Gathering not found"))

        val amount = when (val parsed = MoneyInputParser.parsePen(amountInput)) {
            is DomainResult.Err -> return parsed
            is DomainResult.Ok -> parsed.value
        }

        if (amount.isZero()) {
            return DomainResult.Err(DomainError.InvalidAmount("Amount must be greater than zero"))
        }

        val paidBy = when (val validated = GatheringParticipantValidator.validatePaidBy(gathering, paidByParticipant)) {
            is DomainResult.Err -> return validated
            is DomainResult.Ok -> validated.value
        }

        val trimmedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        val now = timeProvider.now()
        val expense = GatheringExpense(
            id = "gathering-expense-${now.toEpochMilli()}",
            gatheringId = gatheringId,
            amount = amount,
            paidByParticipant = paidBy,
            description = trimmedDescription,
            recordedAt = now,
        )

        return when (val validation = expense.validate()) {
            is DomainResult.Err -> validation
            is DomainResult.Ok -> {
                gatheringExpenseRepository.save(expense).getOrElse {
                    return DomainResult.Err(DomainError.InvalidField("Could not save expense"))
                }
                DomainResult.Ok(expense)
            }
        }
    }
}
