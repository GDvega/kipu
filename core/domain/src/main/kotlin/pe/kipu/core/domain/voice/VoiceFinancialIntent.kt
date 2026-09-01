package pe.kipu.core.domain.voice

import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.receipt.ServiceReceiptKey

sealed interface VoiceFinancialIntent {
    val rawText: String

    data class Expense(
        override val rawText: String,
        val amount: Money,
        val categoryId: String,
        val description: String,
        val channel: PaymentChannel = PaymentChannel.CASH,
        val counterpartyName: String? = null,
        val matchedServiceKey: ServiceReceiptKey? = null,
        val envelopeId: String? = null,
    ) : VoiceFinancialIntent

    data class Income(
        override val rawText: String,
        val amount: Money,
        val categoryId: String,
        val description: String,
        val channel: PaymentChannel = PaymentChannel.CASH,
        val counterpartyName: String? = null,
    ) : VoiceFinancialIntent

    data class GoalContribution(
        override val rawText: String,
        val amount: Money,
        val goalQuery: String,
        val description: String,
    ) : VoiceFinancialIntent

    data class ServiceReceiptPayment(
        override val rawText: String,
        val serviceKey: ServiceReceiptKey,
        val amount: Money?,
        val description: String,
    ) : VoiceFinancialIntent

    data class Unknown(
        override val rawText: String,
    ) : VoiceFinancialIntent
}
