package pe.kipu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val targetAmountCents: Long?,
    val currentAmountCents: Long?,
    /** [java.time.LocalDate.toEpochDay] when present. */
    val dueDateEpochDay: Long?,
    val counterpartyName: String?,
    val isSettled: Boolean,
    val currencyCode: String = "PEN",
    val savingsHorizonMonths: Int? = null,
)
