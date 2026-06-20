package pe.kipu.core.domain.model

enum class SettlementDirection {
    RECEIVES,
    OWES,
    SETTLED,
}

data class ParticipantSettlement(
    val participantName: String,
    val paidAmount: Money,
    val fairShare: Money,
    val balanceDirection: SettlementDirection,
    /** Always non-negative; use [balanceDirection] for sign. */
    val balanceAmount: Money,
)
