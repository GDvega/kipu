package pe.kipu.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.core.data.repository.RoomCategoryRepository
import pe.kipu.core.data.repository.RoomCommitmentRepository
import pe.kipu.core.data.repository.RoomMovementRepository
import pe.kipu.core.data.export.AndroidUserDataExportFileRepository
import pe.kipu.core.data.preferences.DataStoreUserPreferencesRepository
import pe.kipu.core.data.repository.RoomDuplicateDismissalRepository
import pe.kipu.core.data.repository.RoomEnvelopeRepository
import pe.kipu.core.data.repository.RoomFinancialPlanRepository
import pe.kipu.core.data.repository.RoomUserDataWipeRepository
import pe.kipu.core.data.repository.RoomGatheringExpenseRepository
import pe.kipu.core.data.repository.RoomGatheringRepository
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.DuplicateDismissalRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.repository.UserDataExportFileRepository
import pe.kipu.core.domain.repository.UserDataWipeRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.repository.MovementRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovementRepository(impl: RoomMovementRepository): MovementRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindEnvelopeRepository(impl: RoomEnvelopeRepository): EnvelopeRepository

    @Binds
    @Singleton
    abstract fun bindDuplicateDismissalRepository(
        impl: RoomDuplicateDismissalRepository,
    ): DuplicateDismissalRepository

    @Binds
    @Singleton
    abstract fun bindCommitmentRepository(impl: RoomCommitmentRepository): CommitmentRepository

    @Binds
    @Singleton
    abstract fun bindGatheringRepository(impl: RoomGatheringRepository): GatheringRepository

    @Binds
    @Singleton
    abstract fun bindGatheringExpenseRepository(
        impl: RoomGatheringExpenseRepository,
    ): GatheringExpenseRepository

    @Binds
    @Singleton
    abstract fun bindFinancialPlanRepository(impl: RoomFinancialPlanRepository): FinancialPlanRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindUserDataWipeRepository(
        impl: RoomUserDataWipeRepository,
    ): UserDataWipeRepository

    @Binds
    @Singleton
    abstract fun bindUserDataExportFileRepository(
        impl: AndroidUserDataExportFileRepository,
    ): UserDataExportFileRepository
}
