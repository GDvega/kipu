package pe.kipu.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.core.data.repository.RoomCategoryRepository
import pe.kipu.core.data.repository.RoomCommitmentRepository
import pe.kipu.core.data.repository.RoomMovementRepository
import pe.kipu.core.data.repository.RoomPlanSetupRepository
import pe.kipu.core.data.repository.RoomReserveEventRepository
import pe.kipu.core.data.export.AndroidUserDataExportFileRepository
import pe.kipu.core.data.preferences.DataStoreUserPreferencesRepository
import pe.kipu.core.data.repository.RoomDuplicateDismissalRepository
import pe.kipu.core.data.repository.RoomEnvelopeRepository
import pe.kipu.core.data.repository.RoomEnvelopePlanRepository
import pe.kipu.core.data.repository.RoomFinancialPlanRepository
import pe.kipu.core.data.repository.RoomUserDataWipeRepository
import pe.kipu.core.data.repository.RoomGatheringExpenseRepository
import pe.kipu.core.data.repository.RoomGatheringRepository
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.DuplicateDismissalRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.EnvelopePlanRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.repository.UserDataExportFileRepository
import pe.kipu.core.domain.repository.UserDataWipeRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.LocalTransactionRunner
import pe.kipu.core.domain.plan.PlanSetupRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovementRepository(impl: RoomMovementRepository): MovementRepository

    @Binds
    @Singleton
    abstract fun bindReserveEventRepository(impl: RoomReserveEventRepository): ReserveEventRepository

    @Binds
    @Singleton
    abstract fun bindLocalTransactionRunner(
        impl: pe.kipu.core.data.repository.RoomLocalTransactionRunner,
    ): LocalTransactionRunner

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindEnvelopeRepository(impl: RoomEnvelopeRepository): EnvelopeRepository

    @Binds
    @Singleton
    abstract fun bindEnvelopePlanRepository(impl: RoomEnvelopePlanRepository): EnvelopePlanRepository

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
    abstract fun bindPlanSetupRepository(impl: RoomPlanSetupRepository): PlanSetupRepository

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

    @Binds
    @Singleton
    abstract fun bindMonthlyServiceReceiptRepository(
        impl: pe.kipu.core.data.repository.RoomMonthlyServiceReceiptRepository,
    ): pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository

    @Binds
    @Singleton
    abstract fun bindMovementAuditRepository(
        impl: pe.kipu.core.data.repository.RoomMovementAuditRepository,
    ): pe.kipu.core.domain.repository.MovementAuditRepository
}
