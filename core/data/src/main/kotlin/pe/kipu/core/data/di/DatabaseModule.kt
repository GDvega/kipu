package pe.kipu.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.KipuDatabaseCallback
import pe.kipu.core.data.local.dao.CategoryDao
import pe.kipu.core.data.local.dao.CommitmentDao
import pe.kipu.core.data.local.dao.DismissedDuplicatePairDao
import pe.kipu.core.data.local.dao.EnvelopeDao
import pe.kipu.core.data.local.dao.FinancialPlanDao
import pe.kipu.core.data.local.dao.GatheringDao
import pe.kipu.core.data.local.dao.GatheringExpenseDao
import pe.kipu.core.data.local.dao.MovementDao
import pe.kipu.core.data.local.dao.ReserveEventDao
import pe.kipu.core.data.local.migration.KipuDatabaseMigrations

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKipuDatabase(
        @ApplicationContext context: Context,
    ): KipuDatabase = Room.databaseBuilder(
        context,
        KipuDatabase::class.java,
        KipuDatabase.DATABASE_NAME,
    )
        .addMigrations(*KipuDatabaseMigrations.ALL)
        .addCallback(KipuDatabaseCallback())
        .build()

    @Provides
    fun provideMovementDao(database: KipuDatabase): MovementDao = database.movementDao()

    @Provides
    fun provideCategoryDao(database: KipuDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideEnvelopeDao(database: KipuDatabase): EnvelopeDao = database.envelopeDao()

    @Provides
    fun provideDismissedDuplicatePairDao(database: KipuDatabase): DismissedDuplicatePairDao =
        database.dismissedDuplicatePairDao()

    @Provides
    fun provideCommitmentDao(database: KipuDatabase): CommitmentDao = database.commitmentDao()

    @Provides
    fun provideFinancialPlanDao(database: KipuDatabase): FinancialPlanDao = database.financialPlanDao()

    @Provides
    fun provideGatheringDao(database: KipuDatabase): GatheringDao = database.gatheringDao()

    @Provides
    fun provideGatheringExpenseDao(database: KipuDatabase): GatheringExpenseDao =
        database.gatheringExpenseDao()

    @Provides
    fun provideMonthlyServiceReceiptDao(database: KipuDatabase): pe.kipu.core.data.local.dao.MonthlyServiceReceiptDao =
        database.monthlyServiceReceiptDao()

    @Provides
    fun provideMovementAuditDao(database: KipuDatabase): pe.kipu.core.data.local.dao.MovementAuditDao =
        database.movementAuditDao()

    @Provides
    fun provideReserveEventDao(database: KipuDatabase): ReserveEventDao = database.reserveEventDao()
}
