package pe.kipu.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pe.kipu.core.data.local.dao.CategoryDao
import pe.kipu.core.data.local.dao.CommitmentDao
import pe.kipu.core.data.local.dao.DismissedDuplicatePairDao
import pe.kipu.core.data.local.dao.EnvelopeDao
import pe.kipu.core.data.local.dao.FinancialPlanDao
import pe.kipu.core.data.local.dao.GatheringDao
import pe.kipu.core.data.local.dao.GatheringExpenseDao
import pe.kipu.core.data.local.dao.MovementDao
import pe.kipu.core.data.local.entity.DismissedDuplicatePairEntity
import pe.kipu.core.data.local.entity.CategoryEntity
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.data.local.entity.EnvelopeEntity
import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.data.local.entity.GatheringEntity
import pe.kipu.core.data.local.entity.GatheringExpenseEntity
import pe.kipu.core.data.local.entity.MovementEntity

@Database(
    entities = [
        MovementEntity::class,
        CategoryEntity::class,
        EnvelopeEntity::class,
        DismissedDuplicatePairEntity::class,
        CommitmentEntity::class,
        FinancialPlanEntity::class,
        GatheringEntity::class,
        GatheringExpenseEntity::class,
    ],
    version = 16,
    exportSchema = true,
)
abstract class KipuDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao

    abstract fun categoryDao(): CategoryDao

    abstract fun envelopeDao(): EnvelopeDao

    abstract fun dismissedDuplicatePairDao(): DismissedDuplicatePairDao

    abstract fun commitmentDao(): CommitmentDao

    abstract fun financialPlanDao(): FinancialPlanDao

    abstract fun gatheringDao(): GatheringDao

    abstract fun gatheringExpenseDao(): GatheringExpenseDao

    companion object {
        const val DATABASE_NAME = "kipu.db"
    }
}
