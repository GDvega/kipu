package pe.kipu.core.domain.util

import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DomainResult

object GatheringParticipantParser {
    fun parse(input: String): DomainResult<List<String>> {
        val names = input
            .lines()
            .flatMap { line -> line.split(',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (names.isEmpty()) {
            DomainResult.Err(DomainError.InvalidField("At least one participant is required"))
        } else {
            DomainResult.Ok(names)
        }
    }
}
