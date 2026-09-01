package pe.kipu.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAlertDialog
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuBadge
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuIconBadge
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuBlue
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuPurple
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptType
import pe.kipu.core.domain.util.MoneyInputParser

@Composable
fun MonthlyReceiptsCard(
    receipts: List<MonthlyServiceReceipt>,
    onMarkReceiptPaid: (MonthlyServiceReceipt, Money) -> Unit,
    onUnmarkReceiptPaid: (MonthlyServiceReceipt) -> Unit,
    financialPlan: FinancialPlan? = null,
    onEditReceipts: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (receipts.isEmpty()) return

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var receiptToPay by remember { mutableStateOf<MonthlyServiceReceipt?>(null) }

    val paidCount = receipts.count { it.isPaid }
    val totalCount = receipts.size
    val allPaid = paidCount == totalCount

    val totalConfigured = receipts.fold(Money.ZERO) { acc, r -> acc + r.configuredAmount }
    val paidAmount = receipts.filter { it.isPaid }.fold(Money.ZERO) { acc, receipt ->
        acc + (receipt.paidAmount ?: receipt.configuredAmount)
    }
    val pendingAmount = receipts.filterNot { it.isPaid }
        .fold(Money.ZERO) { acc, receipt -> acc + receipt.configuredAmount }

    val monthlyIncome = financialPlan?.estimatedMonthlyIncome ?: Money.ZERO
    val freeForEnvelopes = if (monthlyIncome.amount > totalConfigured.amount) {
        when (val res = monthlyIncome.minus(totalConfigured)) {
            is DomainResult.Ok -> res.value
            is DomainResult.Err -> Money.ZERO
        }
    } else {
        Money.ZERO
    }

    val targetProgress = paidCount.toFloat() / totalCount

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "fixedExpensesProgress",
    )
    val percentInt = (targetProgress * 100).toInt()

    KipuCard(modifier = modifier.fillMaxWidth()) {
        // Cabecera Principal Plegable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    role = Role.Button,
                    onClick = { isExpanded = !isExpanded },
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pagos mensuales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tu plan es una referencia; registra el monto real",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KipuBadge(
                    text = if (allPaid) "✓ 100% Pagado" else "$paidCount/$totalCount pagados",
                    tone = if (allPaid) KipuBadgeTone.Primary else KipuBadgeTone.Warning,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Plegar recibos" else "Desplegar recibos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (isExpanded) 180f else 0f),
                )
            }
        }

        // Vista compacta cuando está plegado
        if (!isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = if (allPaid) KipuPrimary else KipuAmber,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatPenAmountForDisplay(paidAmount.amount)} pagados · Plan ${formatPenAmountForDisplay(totalConfigured.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$percentInt% completado",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allPaid) KipuPrimary else KipuAmber,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Contenido desplegable detallado
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (onEditReceipts != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        KipuTextLink(
                            text = "Editar referencias",
                            onClick = onEditReceipts,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

        // Bloque de Descuento del Sueldo Mensual (si está configurado)
        if (!monthlyIncome.isZero()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Referencia de tu plan mensual",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Plan mensual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Fila de operación matemática visual: Sueldo - Gastos Fijos = Libre para Sobres
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Sueldo recibido",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPenAmountForDisplay(monthlyIncome.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KipuRed,
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Pagos estimados",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPenAmountForDisplay(totalConfigured.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = KipuRed,
                            )
                        }

                        Text(
                            text = "=",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KipuPrimary,
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Disponible planificado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPenAmountForDisplay(freeForEnvelopes.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = KipuPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tarjeta de Consumo con Micro-Métricas
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Progreso visual y porcentaje
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (allPaid) KipuPrimary.copy(alpha = 0.15f) else KipuAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (allPaid) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (allPaid) KipuPrimary else KipuAmber,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Text(
                            text = "Consumo mensual",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "$percentInt% completado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (allPaid) KipuPrimary else KipuAmber,
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = if (allPaid) KipuPrimary else KipuAmber,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    strokeCap = StrokeCap.Round,
                )

                // 3 Micro-Tarjetas de métricas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricChip(
                        title = "Pagado",
                        amountText = formatPenAmountForDisplay(paidAmount.amount),
                        accentColor = KipuPrimary,
                        backgroundColor = KipuPrimary.copy(alpha = 0.08f),
                        borderColor = KipuPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.weight(1f),
                    )
                    MetricChip(
                        title = "Pendiente",
                        amountText = formatPenAmountForDisplay(pendingAmount.amount),
                        accentColor = if (allPaid) MaterialTheme.colorScheme.onSurfaceVariant else KipuAmber,
                        backgroundColor = if (allPaid) MaterialTheme.colorScheme.surface else KipuAmber.copy(alpha = 0.08f),
                        borderColor = if (allPaid) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else KipuAmber.copy(alpha = 0.25f),
                        modifier = Modifier.weight(1f),
                    )
                    MetricChip(
                        title = "Plan ref.",
                        amountText = formatPenAmountForDisplay(totalConfigured.amount),
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Lista de Recibos Individuales
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            receipts.forEach { receipt ->
                MonthlyReceiptRow(
                    receipt = receipt,
                    onTogglePaid = {
                        if (receipt.isPaid) {
                            onUnmarkReceiptPaid(receipt)
                        } else {
                            receiptToPay = receipt
                        }
                    },
                )
            }
        }
            }
        }
    }

    receiptToPay?.let { receipt ->
        ActualServicePaymentDialog(
            receipt = receipt,
            onConfirm = { amount ->
                onMarkReceiptPaid(receipt, amount)
                receiptToPay = null
            },
            onDismiss = { receiptToPay = null },
        )
    }
}

@Composable
private fun ActualServicePaymentDialog(
    receipt: MonthlyServiceReceipt,
    onConfirm: (Money) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable(receipt.key.identifier) {
        mutableStateOf(receipt.configuredAmount.amount.stripTrailingZeros().toPlainString())
    }
    val amount = when (val result = MoneyInputParser.parsePen(amountText)) {
        is DomainResult.Ok -> result.value.takeUnless(Money::isZero)
        is DomainResult.Err -> null
    }

    KipuAlertDialog(
        title = "Registrar pago de ${receipt.title}",
        onDismissRequest = onDismiss,
        dismissText = "Cancelar",
        onDismiss = onDismiss,
        confirmText = "Registrar pago",
        confirmEnabled = amount != null,
        onConfirm = { amount?.let(onConfirm) },
        textContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Referencia del plan: ${formatPenAmountForDisplay(receipt.configuredAmount.amount)}. Ingresa lo que pagaste realmente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KipuPenOutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Monto pagado",
                    placeholder = "0.00",
                    errorText = if (amount == null) "Ingresa un monto válido" else null,
                )
            }
        },
    )
}

@Composable
private fun MetricChip(
    title: String,
    amountText: String,
    accentColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun MonthlyReceiptRow(
    receipt: MonthlyServiceReceipt,
    onTogglePaid: () -> Unit,
) {
    val (icon, tint) = receipt.iconAndTint()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (receipt.isPaid) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (receipt.isPaid) {
                KipuPrimary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KipuIconBadge(
                icon = icon,
                tint = tint,
                size = 42.dp,
                iconSize = 22.dp,
                cornerRadius = 12.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (receipt.isPaid) {
                        "Pagaste ${formatPenAmountForDisplay((receipt.paidAmount ?: receipt.configuredAmount).amount)}"
                    } else {
                        "Referencia ${formatPenAmountForDisplay(receipt.configuredAmount.amount)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (receipt.isPaid) {
                    Text(
                        text = "Plan: ${formatPenAmountForDisplay(receipt.configuredAmount.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (receipt.isPaid) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KipuPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, KipuPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(role = Role.Button, onClick = onTogglePaid),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Pagado (toca para desmarcar)",
                            tint = KipuPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Pagado",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KipuPrimary,
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KipuAmber.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, KipuAmber.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(role = Role.Button, onClick = onTogglePaid),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "Pagar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = KipuAmber,
                        )
                    }
                }
            }
        }
    }
}

private fun MonthlyServiceReceipt.iconAndTint(): Pair<ImageVector, Color> = when (key.type) {
    ServiceReceiptType.LIGHT -> Pair(Icons.Default.Bolt, KipuAmber)
    ServiceReceiptType.WATER -> Pair(Icons.Default.WaterDrop, Color(0xFF0284C7))
    ServiceReceiptType.INTERNET -> Pair(Icons.Default.Wifi, Color(0xFF06B6D4))
    ServiceReceiptType.PHONE -> Pair(Icons.Default.PhoneAndroid, Color(0xFF10B981))
    ServiceReceiptType.RENT -> Pair(Icons.Default.Home, Color(0xFFF97316))
    ServiceReceiptType.DEBTS -> Pair(Icons.Default.Handshake, KipuRed)
    ServiceReceiptType.EDUCATION -> Pair(Icons.Default.School, KipuPurple)
    ServiceReceiptType.CUSTOM -> Pair(Icons.Default.Receipt, KipuBlue)
}
