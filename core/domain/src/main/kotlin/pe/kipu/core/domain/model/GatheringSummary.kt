package pe.kipu.core.domain.model

data class GatheringSummary(
    val gathering: Gathering,
    val totalExpenses: Money,
    val perPersonAmount: Money,
    val settlements: List<ParticipantSettlement>,
)
