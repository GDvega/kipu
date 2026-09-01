package pe.kipu.feature.commitments.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.kipuScrollbar
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.domain.model.CommitmentType

data class CommitmentFormState(
    val type: CommitmentType = CommitmentType.SAVINGS_GOAL,
    val title: String = "",
    val targetAmountText: String = "",
    val currentAmountText: String = "",
    val counterpartyName: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val editingCommitmentId: String? = null,
)

@Composable
fun CommitmentFormDialog(
    formState: CommitmentFormState,
    onTypeSelected: (CommitmentType) -> Unit,
    onTitleChanged: (String) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onCurrentAmountChanged: (String) -> Unit,
    onCounterpartyChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = formState.editingCommitmentId != null
    val types = listOf(
        CommitmentType.SAVINGS_GOAL,
        CommitmentType.SOCIAL_DEBT,
        CommitmentType.PENDING_PAYMENT,
    )
    val typeLabels = listOf("Meta", "Deuda social", "Pago pendiente")
    val targetAmountError = formState.errorMessage.takeIf { it == "Meta inválida" }
    val currentAmountError = formState.errorMessage.takeIf { it == "Monto inválido" }

    AlertDialog(
        onDismissRequest = { if (!formState.isSaving) onDismiss() },
        title = { Text(text = if (isEdit) "Editar compromiso" else "Nuevo compromiso") },
        text = {
            val commitmentDialogScrollState = androidx.compose.foundation.rememberScrollState()
            Column(
                modifier = Modifier
                    .kipuScrollbar(commitmentDialogScrollState)
                    .verticalScroll(commitmentDialogScrollState)
                    .imePadding(),
            ) {
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                KipuFilterChipRow(
                    labels = typeLabels,
                    selectedIndex = types.indexOf(formState.type).coerceAtLeast(0),
                    onSelected = { index -> onTypeSelected(types[index]) },
                    contentPadding = PaddingValues(0.dp),
                    enabled = !formState.isSaving,
                )
                OutlinedTextField(
                    value = formState.title,
                    onValueChange = onTitleChanged,
                    label = { Text("Título") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    enabled = !formState.isSaving,
                    singleLine = true,
                )
                when (formState.type) {
                    CommitmentType.SAVINGS_GOAL -> {
                        KipuPenOutlinedTextField(
                            value = formState.targetAmountText,
                            onValueChange = onTargetAmountChanged,
                            label = "Meta total",
                            modifier = Modifier.padding(top = 12.dp),
                            errorText = targetAmountError,
                            enabled = !formState.isSaving,
                        )
                        KipuPenOutlinedTextField(
                            value = formState.currentAmountText,
                            onValueChange = onCurrentAmountChanged,
                            label = "Ya ahorrado (opcional)",
                            modifier = Modifier.padding(top = 8.dp),
                            errorText = currentAmountError,
                            enabled = !formState.isSaving,
                        )
                    }

                    CommitmentType.SOCIAL_DEBT -> {
                        OutlinedTextField(
                            value = formState.counterpartyName,
                            onValueChange = onCounterpartyChanged,
                            label = { Text("Persona") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            enabled = !formState.isSaving,
                            singleLine = true,
                        )
                        KipuPenOutlinedTextField(
                            value = formState.currentAmountText,
                            onValueChange = onCurrentAmountChanged,
                            label = "Monto pendiente",
                            modifier = Modifier.padding(top = 8.dp),
                            errorText = currentAmountError,
                            enabled = !formState.isSaving,
                        )
                    }

                    CommitmentType.PENDING_PAYMENT -> {
                        KipuPenOutlinedTextField(
                            value = formState.currentAmountText,
                            onValueChange = onCurrentAmountChanged,
                            label = "Monto a pagar",
                            modifier = Modifier.padding(top = 12.dp),
                            errorText = currentAmountError,
                            enabled = !formState.isSaving,
                        )
                    }
                }
                formState.errorMessage
                    ?.takeUnless { it == targetAmountError || it == currentAmountError }
                    ?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics {
                                error(message)
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar",
                onClick = onConfirm,
                enabled = !formState.isSaving,
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
fun CommitmentDeleteConfirmDialog(
    title: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(text = "Eliminar compromiso") },
        text = {
            Column {
                Text(text = "¿Eliminar \"$title\"? Esta acción no se puede deshacer.")
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics {
                                error(message)
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (isDeleting) "Eliminando..." else "Eliminar",
                onClick = onConfirm,
                enabled = !isDeleting,
                destructive = true,
            )
        },
        dismissButton = {
            KipuDialogDismissButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !isDeleting,
            )
        },
    )
}
