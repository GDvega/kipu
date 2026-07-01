package pe.kipu.core.domain.model

data class AutoApprovalPolicy(
    val enabled: Boolean,
    val requiresOperationNumber: Boolean = true,
    val requiresNoDuplicate: Boolean = true,
)
