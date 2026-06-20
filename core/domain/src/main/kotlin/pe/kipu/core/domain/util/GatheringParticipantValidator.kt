package pe.kipu.core.domain.util

import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering

object GatheringParticipantValidator {
    fun validatePaidBy(gathering: Gathering, paidByParticipant: String): DomainResult<String> {
        val trimmed = paidByParticipant.trim()
        if (trimmed.isEmpty()) {
            return DomainResult.Err(DomainError.InvalidField("Paid-by participant is required"))
        }
        val matches = gathering.participantNames.any { it.equals(trimmed, ignoreCase = true) }
        return if (matches) {
            DomainResult.Ok(gathering.participantNames.first { it.equals(trimmed, ignoreCase = true) })
        } else {
            DomainResult.Err(DomainError.InvalidField("Participant must belong to the gathering"))
        }
    }
}
