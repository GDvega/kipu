package pe.kipu.core.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.seed.DefaultCategorySeed

object KipuDatabaseSeeder {
    fun seedBaseline(db: SupportSQLiteDatabase) {
        DefaultCategorySeed.insertInto(db)
    }
}
