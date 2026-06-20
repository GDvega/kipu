package pe.kipu.core.domain.model

data class GatheringsDashboard(
    val summaries: List<GatheringSummary>,
    val unlinkedMovements: List<Movement>,
)
