package pe.kipu.feature.movements.presentation

import androidx.compose.ui.graphics.Color
import java.time.Instant
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuAmberDim
import pe.kipu.core.designsystem.theme.KipuBlue
import pe.kipu.core.designsystem.theme.KipuBlueDim
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuPrimaryDim
import pe.kipu.core.designsystem.theme.KipuPurple
import pe.kipu.core.designsystem.theme.KipuPurpleDim
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel

enum class MovementChannelFilter(val label: String) {
    ALL("Todos"),
    YAPE("Yape"),
    PLIN("Plin"),
    BANK("Bancos"),
    CASH("Efectivo"),
}

data class MovementChannelVisual(
    val iconBackground: Color,
    val iconTint: Color,
)

fun formatMovementDate(instant: Instant): String = MovementDisplayLabels.formatDate(instant)

fun formatMovementDateTime(instant: Instant): String = MovementDisplayLabels.formatDateTime(instant)

fun movementDisplayTitle(
    counterpartyName: String?,
    description: String?,
): String = MovementDisplayLabels.displayTitle(counterpartyName, description)

fun Movement.matchesChannelFilter(filter: MovementChannelFilter): Boolean = when (filter) {
    MovementChannelFilter.ALL -> true
    MovementChannelFilter.YAPE -> channel == PaymentChannel.YAPE
    MovementChannelFilter.PLIN -> channel == PaymentChannel.PLIN
    MovementChannelFilter.BANK -> channel == PaymentChannel.MANUAL || channel == PaymentChannel.OTHER
    MovementChannelFilter.CASH -> channel == PaymentChannel.CASH
}

fun Movement.matchesCategoryFilter(categoryId: String?): Boolean =
    categoryId.isNullOrBlank() || this.categoryId == categoryId

fun Movement.channelLabel(): String = when (channel) {
    PaymentChannel.YAPE -> "Yape"
    PaymentChannel.PLIN -> "Plin"
    PaymentChannel.CASH -> "Efectivo"
    PaymentChannel.MANUAL -> "Banco"
    PaymentChannel.OTHER -> "Otro"
}

fun Movement.sourceLabel(): String = when (source) {
    MovementSource.RECEIPT -> "Comprobante"
    MovementSource.NOTIFICATION -> "Notificación"
    MovementSource.MANUAL -> "Registro manual"
}

fun Movement.sourceBadgeTone(): KipuBadgeTone = when (source) {
    MovementSource.RECEIPT -> KipuBadgeTone.Purple
    MovementSource.NOTIFICATION -> KipuBadgeTone.Primary
    MovementSource.MANUAL -> KipuBadgeTone.Warning
}

fun Movement.statusLabel(): String = when (status) {
    MovementStatus.CONFIRMED -> "Confirmado"
    MovementStatus.PENDING_CONFIRMATION -> "Pendiente de revisar"
}

fun Movement.statusBadgeTone(): KipuBadgeTone = when (status) {
    MovementStatus.CONFIRMED -> KipuBadgeTone.Primary
    MovementStatus.PENDING_CONFIRMATION -> KipuBadgeTone.Warning
}

/** UI labels for movement confidence. MVP uses source-based heuristics; [Movement] has no stored confidence (F14-07 accepted). */
fun Movement.confidenceLabel(): String = when (source) {
    MovementSource.MANUAL -> "100% seguro"
    MovementSource.RECEIPT -> if (operationNumber.isNullOrBlank()) "85% seguro" else "92% seguro"
    MovementSource.NOTIFICATION -> "88% seguro"
}

fun Movement.confidenceIsLow(): Boolean =
    source == MovementSource.RECEIPT && operationNumber.isNullOrBlank()

fun Movement.channelVisual(): MovementChannelVisual = when (channel) {
    PaymentChannel.YAPE -> MovementChannelVisual(KipuPurpleDim, KipuPurple)
    PaymentChannel.PLIN -> MovementChannelVisual(KipuBlueDim, KipuBlue)
    PaymentChannel.CASH -> MovementChannelVisual(KipuAmberDim, KipuAmber)
    PaymentChannel.MANUAL,
    PaymentChannel.OTHER,
    -> MovementChannelVisual(KipuPrimaryDim, KipuPrimary)
}

fun Movement.isIncome(): Boolean = type == MovementType.INCOME

fun pe.kipu.core.domain.model.MovementAuditEntry.actionLabel(): String = when (action) {
    pe.kipu.core.domain.model.MovementAuditAction.CREATED ->
        if (movementType == MovementType.INCOME) "Ingreso registrado" else "Pago registrado"
    pe.kipu.core.domain.model.MovementAuditAction.UPDATED -> "Editado"
    pe.kipu.core.domain.model.MovementAuditAction.DELETED -> "Eliminado"
}

fun pe.kipu.core.domain.model.MovementAuditEntry.actionBadgeTone(): KipuBadgeTone = when (action) {
    pe.kipu.core.domain.model.MovementAuditAction.CREATED ->
        if (movementType == MovementType.INCOME) KipuBadgeTone.Primary else KipuBadgeTone.Info
    pe.kipu.core.domain.model.MovementAuditAction.UPDATED -> KipuBadgeTone.Warning
    pe.kipu.core.domain.model.MovementAuditAction.DELETED -> KipuBadgeTone.Critical
}

fun pe.kipu.core.domain.model.MovementAuditEntry.channelVisual(): MovementChannelVisual = when (channel) {
    PaymentChannel.YAPE -> MovementChannelVisual(KipuPurpleDim, KipuPurple)
    PaymentChannel.PLIN -> MovementChannelVisual(KipuBlueDim, KipuBlue)
    PaymentChannel.CASH -> MovementChannelVisual(KipuAmberDim, KipuAmber)
    PaymentChannel.MANUAL,
    PaymentChannel.OTHER,
    -> MovementChannelVisual(KipuPrimaryDim, KipuPrimary)
}
