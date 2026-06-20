package pe.kipu.core.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.seed.DefaultCategorySeed
import pe.kipu.core.data.local.seed.DefaultCommitmentSeed
import pe.kipu.core.data.local.seed.DefaultEnvelopeSeed
import pe.kipu.core.data.local.seed.DefaultFinancialPlanSeed

object KipuDatabaseSeeder {
    fun seedBaseline(db: SupportSQLiteDatabase) {
        DefaultCategorySeed.insertInto(db)
        DefaultEnvelopeSeed.insertInto(db)
        DefaultCommitmentSeed.insertInto(db)
        DefaultFinancialPlanSeed.insertInto(db)
    }
}
