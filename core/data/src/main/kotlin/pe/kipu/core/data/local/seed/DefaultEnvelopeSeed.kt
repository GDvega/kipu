package pe.kipu.core.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.entity.EnvelopeEntity

import pe.kipu.core.domain.category.CategoryIds

object DefaultEnvelopeSeed {
    val envelopes: List<EnvelopeEntity> = listOf(
        EnvelopeEntity(
            id = DefaultEnvelopeIds.FOOD,
            name = "Comida",
            weeklyLimitCents = 15_000L,
            categoryId = CategoryIds.FOOD,
        ),
        EnvelopeEntity(
            id = DefaultEnvelopeIds.TRANSPORT,
            name = "Transporte",
            weeklyLimitCents = 8_000L,
            categoryId = CategoryIds.TRANSPORT,
        ),
        EnvelopeEntity(
            id = DefaultEnvelopeIds.SERVICES,
            name = "Servicios",
            weeklyLimitCents = 6_000L,
            categoryId = CategoryIds.SERVICES,
        ),
        EnvelopeEntity(
            id = DefaultEnvelopeIds.LEISURE,
            name = "Ocio",
            weeklyLimitCents = 8_000L,
            categoryId = CategoryIds.OTHER,
        ),
        EnvelopeEntity(
            id = DefaultEnvelopeIds.FAMILY,
            name = "Familia",
            weeklyLimitCents = 10_000L,
            categoryId = CategoryIds.OTHER,
        ),
        EnvelopeEntity(
            id = DefaultEnvelopeIds.ANT_SPENDING,
            name = "Gastos hormiga",
            weeklyLimitCents = 3_500L,
            categoryId = CategoryIds.OTHER,
        ),
    )

    fun insertInto(db: SupportSQLiteDatabase) {
        envelopes.forEach { envelope ->
            db.execSQL(
                "INSERT OR IGNORE INTO envelopes (id, name, weeklyLimitCents, categoryId) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(
                    envelope.id,
                    envelope.name,
                    envelope.weeklyLimitCents,
                    envelope.categoryId,
                ),
            )
        }
    }
}
