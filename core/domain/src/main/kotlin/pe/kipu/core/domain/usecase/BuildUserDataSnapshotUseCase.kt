package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.export.UserDataSnapshot
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.DuplicateDismissalRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.TimeProvider

class BuildUserDataSnapshotUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val categoryRepository: CategoryRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val commitmentRepository: CommitmentRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val gatheringRepository: GatheringRepository,
    private val duplicateDismissalRepository: DuplicateDismissalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(): UserDataSnapshot = UserDataSnapshot(
        exportedAt = timeProvider.now(),
        movements = movementRepository.observeMovements().first(),
        categories = categoryRepository.observeCategories().first(),
        envelopes = envelopeRepository.observeEnvelopes().first(),
        commitments = commitmentRepository.observeCommitments().first(),
        financialPlans = financialPlanRepository.observePlans().first(),
        gatherings = gatheringRepository.observeGatherings().first(),
        dismissedDuplicatePairKeys = duplicateDismissalRepository.observeDismissedPairKeys().first(),
        preferences = userPreferencesRepository.observePreferences().first(),
    )
}
