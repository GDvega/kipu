package pe.kipu.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.feature.movements.presentation.ManualMovementChannelOption
import pe.kipu.feature.movements.presentation.ManualMovementAmountValidator

data class ManualMovementFormState(
    val movementType: MovementType = MovementType.EXPENSE,
    val channel: PaymentChannel = PaymentChannel.CASH,
    val amountText: String = "",
    val categoryId: String? = null,
    val description: String = "",
    val counterpartyName: String = "",
    val isSaving: Boolean = false,
    val amountErrorMessage: String? = ManualMovementAmountValidator.EMPTY_AMOUNT_MESSAGE,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = !isSaving && categoryId != null && amountErrorMessage == null
}

@Composable
fun ManualMovementDialog(
    categories: List<Category>,
    formState: ManualMovementFormState,
    onMovementTypeSelected: (MovementType) -> Unit,
    onChannelSelected: (ManualMovementChannelOption) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCounterpartyChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Registrar movimiento")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                KipuFilterChipRow(
                    labels = listOf("Gasto", "Ingreso"),
                    selectedIndex = if (formState.movementType == MovementType.EXPENSE) 0 else 1,
                    onSelected = { index ->
                        onMovementTypeSelected(
                            if (index == 0) MovementType.EXPENSE else MovementType.INCOME,
                        )
                    },
                    contentPadding = PaddingValues(0.dp),
                )

                Text(
                    text = "Canal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                KipuFilterChipRow(
                    labels = ManualMovementChannelOption.entries.map { it.label },
                    selectedIndex = ManualMovementChannelOption.entries.indexOfFirst {
                        it.channel == formState.channel
                    }.coerceAtLeast(0),
                    onSelected = { index ->
                        onChannelSelected(ManualMovementChannelOption.entries[index])
                    },
                    contentPadding = PaddingValues(0.dp),
                )

                KipuPenOutlinedTextField(
                    value = formState.amountText,
                    onValueChange = onAmountChanged,
                    label = "Monto",
                )
                formState.amountErrorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (categories.isNotEmpty()) {
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    KipuFilterChipRow(
                        labels = categories.map { it.name },
                        selectedIndex = categories.indexOfFirst { it.id == formState.categoryId }
                            .coerceAtLeast(0),
                        onSelected = { index -> onCategorySelected(categories[index].id) },
                        contentPadding = PaddingValues(0.dp),
                    )
                }

                OutlinedTextField(
                    value = formState.counterpartyName,
                    onValueChange = onCounterpartyChanged,
                    label = { Text("Persona o lugar (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                formState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar",
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
