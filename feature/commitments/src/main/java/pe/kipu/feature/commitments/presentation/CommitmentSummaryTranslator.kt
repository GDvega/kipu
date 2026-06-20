package pe.kipu.feature.commitments.presentation

import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.CommitmentStatusKeys
import pe.kipu.core.domain.model.CommitmentSummary
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.FinancialPlanValidationResult

object CommitmentSummaryTranslator {

    fun statusText(summary: CommitmentSummary): String = when (summary.statusKey) {
        CommitmentStatusKeys.SAVINGS_IN_PROGRESS ->
            "Meta de ahorro en progreso"

        CommitmentStatusKeys.SAVINGS_COMPLETED ->
            "Meta de ahorro completada"

        CommitmentStatusKeys.SOCIAL_DEBT_PENDING ->
            "Deuda social pendiente"

        CommitmentStatusKeys.SOCIAL_DEBT_SETTLED ->
            "Deuda social saldada"

        CommitmentStatusKeys.PENDING_PAYMENT_DUE ->
            "Pago pendiente"

        CommitmentStatusKeys.PENDING_PAYMENT_SETTLED ->
            "Pago registrado"

        else ->
            "Compromiso activo"
    }

    fun savingsProgressText(summary: CommitmentSummary): String? {
        val progress = summary.savingsProgress ?: return null
        return "${progress.progressPercent}% · " +
            "${formatPenAmountForDisplay(progress.savedAmount.amount)} de " +
            formatPenAmountForDisplay(progress.targetAmount.amount)
    }

    fun amountLabel(summary: CommitmentSummary): String? = when (summary.commitment.type) {
        CommitmentType.SAVINGS_GOAL -> null

        CommitmentType.SOCIAL_DEBT,
        CommitmentType.PENDING_PAYMENT,
        -> summary.commitment.currentAmount?.let { amount ->
            formatPenAmountForDisplay(amount.amount)
        }
    }

    fun planValidationText(validation: FinancialPlanValidationResult): String? = when (validation) {
        FinancialPlanValidationResult.Valid -> null

        is FinancialPlanValidationResult.Invalid ->
            "Tu plan queda en negativo por " +
                formatPenAmountForDisplay(validation.deficit.amount) +
                " al mes. Revisa ingresos, gastos fijos, sobres y compromisos."
    }
}
