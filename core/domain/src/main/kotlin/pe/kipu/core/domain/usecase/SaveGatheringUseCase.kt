package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.util.GatheringParticipantParser

class SaveGatheringUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(name: String, participantsInput: String): DomainResult<Gathering> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return DomainResult.Err(
                pe.kipu.core.domain.model.DomainError.InvalidField("Gathering name is required"),
            )
        }

        val participants = when (val parsed = GatheringParticipantParser.parse(participantsInput)) {
            is DomainResult.Err -> return parsed
            is DomainResult.Ok -> parsed.value
        }

        val gathering = Gathering(
            id = "gathering-${timeProvider.now().toEpochMilli()}",
            name = trimmedName,
            participantCount = participants.size,
            participantNames = participants,
        )

        return when (val validation = gathering.validate()) {
            is DomainResult.Err -> validation
            is DomainResult.Ok -> {
                gatheringRepository.save(gathering).getOrElse {
                    return DomainResult.Err(
                        pe.kipu.core.domain.model.DomainError.InvalidField("Could not save gathering"),
                    )
                }
                DomainResult.Ok(gathering)
            }
        }
    }
}
