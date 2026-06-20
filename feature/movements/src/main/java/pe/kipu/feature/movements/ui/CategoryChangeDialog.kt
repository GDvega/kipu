package pe.kipu.feature.movements.ui

import androidx.compose.foundation.clickable
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
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                LazyColumn {
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
                                .clickable { onCategorySelected(category.id) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
