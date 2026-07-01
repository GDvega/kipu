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
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Movement

@Composable
fun GoalLinkDialog(
    movement: Movement,
    savingsGoals: List<Commitment>,
    currentGoalTitle: String?,
    onGoalSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Vincular a meta") },
        text = {
            Column {
                Text(
                    text = "Ingreso: ${movement.counterpartyName ?: movement.description ?: "Sin nombre"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (currentGoalTitle != null) {
                    Text(
                        text = "Meta actual: $currentGoalTitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                if (savingsGoals.isEmpty()) {
                    Text(
                        text = "No tienes metas de ahorro. Crea una en Compromisos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        if (movement.commitmentId != null) {
                            item(key = "unlink") {
                                Text(
                                    text = "Quitar vínculo",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGoalSelected(null) }
                                        .padding(vertical = 12.dp),
                                )
                            }
                        }
                        items(savingsGoals, key = { it.id }) { goal ->
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (goal.id == movement.commitmentId) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGoalSelected(goal.id) }
                                    .padding(vertical = 12.dp),
                            )
                        }
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
