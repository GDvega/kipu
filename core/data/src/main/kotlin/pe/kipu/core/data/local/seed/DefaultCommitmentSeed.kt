package pe.kipu.core.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.plan.CommitmentIds

object DefaultCommitmentSeed {
    /** Demo seed only — not the wizard's primary social-debt slot. */
    private const val DEMO_SOCIAL_DEBT_JUAN_ID = "commitment-debt-juan"

    val commitments: List<CommitmentEntity> = listOf(
        CommitmentEntity(
            id = CommitmentIds.EMERGENCY_FUND,
            type = CommitmentType.SAVINGS_GOAL.name,
            title = "Fondo emergencia",
            targetAmountCents = 50_000L,
            currentAmountCents = 12_000L,
            dueDateEpochDay = null,
            counterpartyName = null,
            isSettled = false,
        ),
        CommitmentEntity(
            id = DEMO_SOCIAL_DEBT_JUAN_ID,
            type = CommitmentType.SOCIAL_DEBT.name,
            title = "Deuda con Juan",
            targetAmountCents = null,
            currentAmountCents = 8_000L,
            dueDateEpochDay = null,
            counterpartyName = "Juan",
            isSettled = false,
        ),
    )

    fun insertInto(db: SupportSQLiteDatabase) {
        commitments.forEach { commitment ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO commitments (
                    id, type, title, targetAmountCents, currentAmountCents,
                    dueDateEpochDay, counterpartyName, isSettled, currencyCode
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                ),
            )
        }
    }
}
