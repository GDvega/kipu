package pe.kipu.feature.movements.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.kipuScrollbar
import pe.kipu.core.designsystem.component.KipuFilterChip
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuBlue
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuPurple
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.feature.movements.presentation.ManualMovementAmountValidator
import pe.kipu.feature.movements.presentation.ManualMovementChannelOption

private val QUICK_AMOUNT_PRESETS = listOf(
    BigDecimal("5"),
    BigDecimal("10"),
    BigDecimal("20"),
    BigDecimal("50"),
    BigDecimal("100"),
)

data class EditMovementFormState(
    val movementId: String,
    val movementType: MovementType,
    val channel: PaymentChannel,
    val amountText: String,
    val categoryId: String?,
    val description: String,
    val counterpartyName: String,
    val isSaving: Boolean = false,
    val amountErrorMessage: String? = null,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = !isSaving &&
            !categoryId.isNullOrBlank() &&
            amountErrorMessage == null &&
            ManualMovementAmountValidator.isValid(amountText)

    companion object {
        fun fromMovement(movement: Movement): EditMovementFormState {
            val amountFormatted = movement.amount.amount.stripTrailingZeros().toPlainString()
            return EditMovementFormState(
                movementId = movement.id,
                movementType = movement.type,
                channel = movement.channel,
                amountText = amountFormatted,
                categoryId = movement.categoryId,
                description = movement.description.orEmpty(),
                counterpartyName = movement.counterpartyName.orEmpty(),
            )
        }
    }
}

@Composable
fun EditMovementDialog(
    categories: List<Category>,
    formState: EditMovementFormState,
    onMovementTypeSelected: (MovementType) -> Unit,
    onChannelSelected: (ManualMovementChannelOption) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCounterpartyChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isExpense = formState.movementType == MovementType.EXPENSE

    AlertDialog(
        onDismissRequest = { if (!formState.isSaving) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isExpense) KipuRed else KipuPrimary),
                )
                Text(
                    text = "Editar Movimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            val editDialogScrollState = androidx.compose.foundation.rememberScrollState()
            Column(
                modifier = Modifier
                    .kipuScrollbar(editDialogScrollState)
                    .verticalScroll(editDialogScrollState)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Tipo de movimiento
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isExp = formState.movementType == MovementType.EXPENSE
                        val isInc = formState.movementType == MovementType.INCOME

                        EditTypeTabButton(
                            title = "Gasto",
                            subtitle = "Salida de dinero",
                            isSelected = isExp,
                            activeColor = KipuRed,
                            onClick = { onMovementTypeSelected(MovementType.EXPENSE) },
                            enabled = !formState.isSaving,
                            modifier = Modifier.weight(1f),
                        )

                        EditTypeTabButton(
                            title = "Ingreso",
                            subtitle = "Entrada de dinero",
                            isSelected = isInc,
                            activeColor = KipuPrimary,
                            onClick = { onMovementTypeSelected(MovementType.INCOME) },
                            enabled = !formState.isSaving,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 2. Canal de pago
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Canal de pago",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ManualMovementChannelOption.entries.forEach { option ->
                            val isSelected = option.channel == formState.channel
                            val channelColor = when (option.channel) {
                                PaymentChannel.CASH -> KipuAmber
                                PaymentChannel.YAPE -> KipuPurple
                                PaymentChannel.PLIN -> KipuBlue
                                PaymentChannel.MANUAL -> KipuPrimary
                                PaymentChannel.OTHER -> MaterialTheme.colorScheme.secondary
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) channelColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable(
                                        enabled = !formState.isSaving,
                                        role = Role.RadioButton,
                                        onClick = { onChannelSelected(option) },
                                    ),
                                color = if (isSelected) channelColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(channelColor),
                                    )
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) channelColor else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Monto
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    KipuPenOutlinedTextField(
                        value = formState.amountText,
                        onValueChange = onAmountChanged,
                        label = "Monto",
                        placeholder = "0.00",
                        errorText = formState.amountErrorMessage,
                        enabled = !formState.isSaving,
                    )

                    Text(
                        text = "Montos rápidos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QUICK_AMOUNT_PRESETS.forEach { preset ->
                            val label = "+S/ ${preset.stripTrailingZeros().toPlainString()}"
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        enabled = !formState.isSaving,
                                        role = Role.Button,
                                        onClick = {
                                            val newAmount = ManualMovementAmountValidator.applyPreset(
                                                formState.amountText,
                                                preset,
                                            )
                                            onAmountChanged(newAmount)
                                        },
                                    ),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                // 4. Sobre / Categoría
                if (categories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Sobre / Categoría",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            categories.forEach { category ->
                                val isSelected = category.id == formState.categoryId
                                KipuFilterChip(
                                    text = category.name,
                                    selected = isSelected,
                                    onClick = { onCategorySelected(category.id) },
                                    enabled = !formState.isSaving,
                                )
                            }
                        }
                    }
                }

                // 5. Detalles opcionales
                OutlinedTextField(
                    value = formState.counterpartyName,
                    onValueChange = onCounterpartyChanged,
                    label = { Text("Persona o lugar (opcional)") },
                    placeholder = { Text("Ej. Casera, Don Pepe, Tambo, Menú") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !formState.isSaving,
                    singleLine = true,
                )

                OutlinedTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Nota (opcional)") },
                    placeholder = { Text("Ej. Almuerzo del día, pasaje") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !formState.isSaving,
                    singleLine = true,
                )

                // Error message
                formState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics {
                            error(message)
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar Cambios",
                onClick = onConfirm,
                enabled = formState.canSave,
            )
        },
        dismissButton = {
            KipuDialogDismissButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !formState.isSaving,
            )
        },
    )
}

@Composable
private fun EditTypeTabButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent,
        label = "editTypeTabBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else Color.Transparent,
        label = "editTypeTabBorder",
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) activeColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
