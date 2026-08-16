package pe.kipu.feature.movements.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Movement

@Composable
fun CategoryChangeDialog(
    movement: Movement,
    categories: List<Category>,
    currentCategoryName: String?,
    isProcessing: Boolean,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text(text = "Cambiar categoría")
        },
        text = {
            Column {
                Text(
                    text = "Movimiento: ${movement.counterpartyName ?: movement.description ?: "Sin nombre"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (currentCategoryName != null) {
                    Text(
                        text = "Actual: $currentCategoryName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                LazyColumn(modifier = Modifier.selectableGroup()) {
                    items(categories, key = { it.id }) { category ->
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (category.id == movement.categoryId) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = category.id == movement.categoryId,
                                    enabled = !isProcessing,
                                    onClick = { onCategorySelected(category.id) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            KipuDialogDismissButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !isProcessing,
            )
        },
    )
}
