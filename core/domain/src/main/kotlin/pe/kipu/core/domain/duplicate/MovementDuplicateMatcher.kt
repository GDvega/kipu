package pe.kipu.core.domain.duplicate

import java.time.Duration
import javax.inject.Inject
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus

/**
 * Pure duplicate comparison for movements. Both sides must be [MovementStatus.CONFIRMED].
 */
class MovementDuplicateMatcher @Inject constructor() {

    fun isDuplicate(candidate: Movement, existing: Movement): Boolean =
        findMatchReasonKey(candidate, existing) != null

    fun findMatchReasonKey(candidate: Movement, existing: Movement): String? {
        if (candidate.id == existing.id) return null
        if (!isEligibleForDuplicateScan(candidate) || !isEligibleForDuplicateScan(existing)) return null

        val operationReason = matchByOperationNumber(candidate, existing)
        if (operationReason != null) return operationReason

        return matchByAmountCounterpartyTime(candidate, existing)
    }

    private fun isEligibleForDuplicateScan(movement: Movement): Boolean =
        movement.status == MovementStatus.CONFIRMED

    private fun matchByOperationNumber(a: Movement, b: Movement): String? {
        val operationA = a.operationNumber?.trim()?.takeIf { it.isNotEmpty() }
        val operationB = b.operationNumber?.trim()?.takeIf { it.isNotEmpty() }
        if (operationA == null || operationB == null) return null
        if (a.amount != b.amount) return null
        return if (operationA.equals(operationB, ignoreCase = true)) {
            DuplicateMatchReasonKeys.OPERATION_NUMBER
        } else {
            null
        }
    }

    private fun matchByAmountCounterpartyTime(a: Movement, b: Movement): String? {
        if (a.amount != b.amount) return null

        val counterpartyA = normalizeCounterparty(a.counterpartyName)
        val counterpartyB = normalizeCounterparty(b.counterpartyName)
        if (counterpartyA == null || counterpartyB == null) return null
        if (counterpartyA != counterpartyB) return null

        val minutesApart = Duration.between(a.recordedAt, b.recordedAt).abs().toMinutes()
        if (minutesApart > DuplicateDetectionConfig.TIME_TOLERANCE_MINUTES) return null

        return DuplicateMatchReasonKeys.AMOUNT_COUNTERPARTY_TIME
    }

    private fun normalizeCounterparty(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return name
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }
}
