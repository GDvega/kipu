package pe.kipu.feature.envelopes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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

data class EnvelopeCreateFormState(
    val name: String = "",
    val selectedCategoryIndex: Int = 0,
    val amountText: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
fun EnvelopeCreateDialog(
    availableCategories: List<Category>,
    formState: EnvelopeCreateFormState,
    onNameChanged: (String) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onAmountChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nuevo sobre") },
        text = {
            Column {
                if (availableCategories.isEmpty()) {
                    Text(
                        text = "Todas las categorías ya tienen un sobre asignado.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    KipuPenOutlinedTextField(
                        value = formState.name,
                        onValueChange = onNameChanged,
                        label = "Nombre del sobre",
                        showPrefix = false,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    )
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    KipuFilterChipRow(
                        labels = availableCategories.map { it.name },
                        selectedIndex = formState.selectedCategoryIndex.coerceIn(availableCategories.indices),
                        onSelected = onCategorySelected,
                        contentPadding = PaddingValues(0.dp),
                    )
                    KipuPenOutlinedTextField(
                        value = formState.amountText,
                        onValueChange = onAmountChanged,
                        label = "Límite semanal",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                formState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (formState.isSaving) "Guardando..." else "Crear",
                onClick = onConfirm,
                enabled = !formState.isSaving && availableCategories.isNotEmpty(),
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
fun EnvelopeDeleteConfirmDialog(
    envelopeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Eliminar sobre") },
        text = {
            Text(
                text = "¿Eliminar el sobre \"$envelopeName\"? Los movimientos de su categoría no se borran.",
            )
        },
        confirmButton = {
            KipuDialogConfirmButton(text = "Eliminar", onClick = onConfirm)
        },
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
