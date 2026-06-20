package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.duplicate.canonicalKey
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.repository.DuplicateDismissalRepository

class DismissDuplicatePairUseCase @Inject constructor(
    private val duplicateDismissalRepository: DuplicateDismissalRepository,
) {

    suspend operator fun invoke(pair: MovementDuplicatePair): Result<Unit> =
        duplicateDismissalRepository.dismiss(pair.canonicalKey())
}
