package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.AutoApprovalPolicy
import pe.kipu.core.domain.model.Movement

class EvaluateAutoApprovalUseCase @Inject constructor() {

    /**
     * Evalúa si un movimiento califica para ser auto-aprobado.
     * @param movement El movimiento a evaluar.
     * @param policy La política de auto-aprobación del usuario.
     * @param hasDuplicates Indica si se detectaron duplicados para este movimiento.
     */
    operator fun invoke(
        movement: Movement,
        policy: AutoApprovalPolicy,
        hasDuplicates: Boolean,
    ): Boolean {
        if (!policy.enabled) return false

        if (movement.amount.amount <= BigDecimal.ZERO) return false

        if (policy.requiresOperationNumber) {
            if (movement.operationNumber.isNullOrBlank()) return false
        }

        if (policy.requiresNoDuplicate) {
            if (hasDuplicates) return false
        }

        return true
    }
}
