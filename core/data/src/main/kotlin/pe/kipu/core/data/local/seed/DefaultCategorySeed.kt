package pe.kipu.core.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.entity.CategoryEntity
import pe.kipu.core.domain.category.CategoryIds

object DefaultCategorySeed {
    val categories: List<CategoryEntity> = listOf(
        CategoryEntity(id = CategoryIds.FOOD, name = "Comida", iconKey = "food"),
        CategoryEntity(id = CategoryIds.TRANSPORT, name = "Transporte", iconKey = "transport"),
        CategoryEntity(id = CategoryIds.SERVICES, name = "Servicios", iconKey = "services"),
        CategoryEntity(id = CategoryIds.OTHER, name = "Otros", iconKey = "other"),
    )

    fun insertInto(db: SupportSQLiteDatabase) {
        categories.forEach { category ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, iconKey) VALUES (?, ?, ?)",
                arrayOf(category.id, category.name, category.iconKey),
            )
        }
    }
}
