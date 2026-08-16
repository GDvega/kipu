package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.util.GatheringParticipantParser

class UpdateGatheringUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
) {

    suspend operator fun invoke(
        id: EntityId,
        name: String,
        participantsInput: String,
    ): DomainResult<Gathering> {
        val existing = gatheringRepository.getById(id)
            ?: return DomainResult.Err(DomainError.NotFound("Gathering not found"))

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return DomainResult.Err(DomainError.InvalidField("Gathering name is required"))
        }

        val participants = when (val parsed = GatheringParticipantParser.parse(participantsInput)) {
            is DomainResult.Err -> return parsed
            is DomainResult.Ok -> parsed.value
        }

        val referencedParticipants = gatheringExpenseRepository.observeExpensesByGathering()
            .first()[id]
            .orEmpty()
            .map { it.paidByParticipant }
            .distinct()
        if (referencedParticipants.any { referenced ->
                participants.none { it.equals(referenced, ignoreCase = true) }
            }
        ) {
            return DomainResult.Err(
                DomainError.InvalidField("No puedes quitar a una persona que ya registró gastos"),
            )
        }

        val gathering = existing.copy(
            name = trimmedName,
            participantCount = participants.size,
            participantNames = participants,
        )

        return when (val validation = gathering.validate()) {
            is DomainResult.Err -> validation
            is DomainResult.Ok -> {
                gatheringRepository.save(gathering).getOrElse {
                    return DomainResult.Err(DomainError.InvalidField("Could not update gathering"))
                }
                DomainResult.Ok(gathering)
            }
        }
    }
}
