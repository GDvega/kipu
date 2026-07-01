package pe.kipu.core.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.plan.CommitmentIds

object DefaultCommitmentSeed {
    val commitments: List<CommitmentEntity> = emptyList()

    fun insertInto(db: SupportSQLiteDatabase) {
        commitments.forEach { commitment ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO commitments (
                    id, type, title, targetAmountCents, currentAmountCents,
                    dueDateEpochDay, counterpartyName, isSettled, currencyCode, savingsHorizonMonths
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    commitment.id,
                    commitment.type,
                    commitment.title,
                    commitment.targetAmountCents,
                    commitment.currentAmountCents,
                    commitment.dueDateEpochDay,
                    commitment.counterpartyName,
                    if (commitment.isSettled) 1 else 0,
                    commitment.currencyCode,
                    null,
                ),
            )
        }
    }
}
