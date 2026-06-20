package pe.kipu.feature.juntas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.domain.model.SettlementDirection
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.GatheringSummary
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.ParticipantSettlement
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.feature.juntas.presentation.GatheringDialogMode
import pe.kipu.feature.juntas.presentation.GatheringsUiState
import pe.kipu.feature.juntas.presentation.GatheringsViewModel

@Composable
fun GatheringsScreen(
    modifier: Modifier = Modifier,
    viewModel: GatheringsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        KipuScreenHeader(
            title = "Juntas",
            subtitle = "Gastos compartidos con amigos",
        )

        when (val state = uiState) {
            GatheringsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    KipuLoadingIndicator()
                }
            }

            is GatheringsUiState.Content -> {
                val participants = when (val mode = state.dialogMode) {
                    is GatheringDialogMode.RecordExpense ->
                        state.summaries.firstOrNull { it.gathering.id == mode.gatheringId }
                            ?.gathering?.participantNames.orEmpty()
                    is GatheringDialogMode.LinkMovement ->
                        state.summaries.firstOrNull { it.gathering.id == mode.gatheringId }
                            ?.gathering?.participantNames.orEmpty()
                    else -> emptyList()
                }

                when (val mode = state.dialogMode) {
                    GatheringDialogMode.Create,
                    is GatheringDialogMode.Edit,
                    -> GatheringFormDialog(
                        title = if (mode is GatheringDialogMode.Edit) "Editar junta" else "Nueva junta",
                        name = state.formName,
                        participants = state.formParticipants,
                        errorMessage = state.formError,
                        isSaving = state.isSaving,
                        onNameChanged = viewModel::onFormNameChanged,
                        onParticipantsChanged = viewModel::onFormParticipantsChanged,
                        onConfirm = viewModel::onConfirmDialog,
                        onDismiss = viewModel::onDismissDialog,
                    )

                    is GatheringDialogMode.RecordExpense -> RecordExpenseDialog(
                        amount = state.formAmount,
                        paidBy = state.formPaidBy,
                        participants = participants,
                        description = state.formDescription,
                        errorMessage = state.formError,
                        isSaving = state.isSaving,
                        onAmountChanged = viewModel::onFormAmountChanged,
                        onPaidByChanged = viewModel::onFormPaidByChanged,
                        onDescriptionChanged = viewModel::onFormDescriptionChanged,
                        onConfirm = viewModel::onConfirmDialog,
                        onDismiss = viewModel::onDismissDialog,
                    )

                    is GatheringDialogMode.LinkMovement -> LinkMovementDialog(
                        movements = state.unlinkedMovements,
                        selectedMovementId = state.formMovementId,
                        paidBy = state.formPaidBy,
                        participants = participants,
                        errorMessage = state.formError,
                        isSaving = state.isSaving,
                        onMovementSelected = viewModel::onFormMovementSelected,
                        onPaidByChanged = viewModel::onFormPaidByChanged,
                        onConfirm = viewModel::onConfirmDialog,
                        onDismiss = viewModel::onDismissDialog,
                    )

                    null -> Unit
                }

                if (state.summaries.isEmpty()) {
                    KipuEmptyState(
                        title = "Sin juntas",
                        message = "Registra salidas, cenas o paseos para repartir gastos después.",
                        actionLabel = "Nueva junta",
                        onAction = viewModel::onCreateClick,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KipuLayout.listItemSpacing),
                        contentPadding = KipuLayout.screenContentPadding(),
                    ) {
                        item {
                            KipuPrimaryButton(
                                text = "Nueva junta",
                                onClick = viewModel::onCreateClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        items(state.summaries, key = { it.gathering.id }) { summary ->
                            GatheringCard(
                                summary = summary,
                                onEdit = { viewModel.onEditClick(summary) },
                                onRecordExpense = { viewModel.onRecordExpenseClick(summary.gathering.id) },
                                onLinkMovement = { viewModel.onLinkMovementClick(summary.gathering.id) },
                                onDelete = { viewModel.onDeleteGathering(summary.gathering.id) },
                            )
                        }
                    }
                }
            }

            is GatheringsUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar tus juntas",
                    message = state.message,
                )
            }
        }
    }
}

@Composable
private fun GatheringCard(
    summary: GatheringSummary,
    onEdit: () -> Unit,
    onRecordExpense: () -> Unit,
    onLinkMovement: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gathering = summary.gathering
    KipuCard(modifier = modifier.fillMaxWidth()) {
        Text(text = gathering.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${gathering.participantCount} participantes",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = gathering.participantNames.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (!summary.totalExpenses.isZero()) {
            Text(
                text = "Total gastado: ${formatPenAmountForDisplay(summary.totalExpenses.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Cuota por persona: ${formatPenAmountForDisplay(summary.perPersonAmount.amount)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            summary.settlements.forEach { settlement ->
                Text(
                    text = settlementLabel(settlement),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        KipuSecondaryButton(
            text = "Registrar gasto",
            onClick = onRecordExpense,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            fillWidth = true,
        )
        KipuSecondaryButton(
            text = "Vincular movimiento",
            onClick = onLinkMovement,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            fillWidth = true,
        )
        KipuSecondaryButton(
            text = "Editar",
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            fillWidth = true,
        )
        KipuSecondaryButton(
            text = "Eliminar",
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            fillWidth = true,
        )
    }
}

private fun settlementLabel(settlement: ParticipantSettlement): String {
    val amount = formatPenAmountForDisplay(settlement.balanceAmount.amount)
    return when (settlement.balanceDirection) {
        SettlementDirection.RECEIVES -> "${settlement.participantName}: le deben S/ $amount"
        SettlementDirection.OWES -> "${settlement.participantName}: debe S/ $amount"
        SettlementDirection.SETTLED -> "${settlement.participantName}: al día"
    }
}

@Composable
private fun GatheringFormDialog(
    title: String,
    name: String,
    participants: String,
    errorMessage: String?,
    isSaving: Boolean,
    onNameChanged: (String) -> Unit,
    onParticipantsChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = participants,
                    onValueChange = onParticipantsChanged,
                    label = { Text("Participantes") },
                    placeholder = { Text("Uno por línea o separados por coma") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                errorMessage?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (isSaving) "Guardando..." else "Guardar",
                onClick = onConfirm,
                enabled = !isSaving,
            )
        },
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss, enabled = !isSaving)
        },
    )
}

@Composable
private fun RecordExpenseDialog(
    amount: String,
    paidBy: String,
    participants: List<String>,
    description: String,
    errorMessage: String?,
    isSaving: Boolean,
    onAmountChanged: (String) -> Unit,
    onPaidByChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Registrar gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KipuPenOutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChanged,
                    label = "Monto",
                )
                ParticipantPicker(
                    participants = participants,
                    selected = paidBy,
                    onSelected = onPaidByChanged,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                errorMessage?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (isSaving) "Guardando..." else "Guardar",
                onClick = onConfirm,
                enabled = !isSaving,
            )
        },
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss, enabled = !isSaving)
        },
    )
}

@Composable
private fun LinkMovementDialog(
    movements: List<Movement>,
    selectedMovementId: String?,
    paidBy: String,
    participants: List<String>,
    errorMessage: String?,
    isSaving: Boolean,
    onMovementSelected: (String) -> Unit,
    onPaidByChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Vincular movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (movements.isEmpty()) {
                    Text(
                        text = "No hay movimientos de gasto confirmados sin vincular.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    movements.take(8).forEach { movement ->
                        MovementOptionRow(
                            movement = movement,
                            selected = movement.id == selectedMovementId,
                            onClick = { onMovementSelected(movement.id) },
                        )
                    }
                }
                ParticipantPicker(
                    participants = participants,
                    selected = paidBy,
                    onSelected = onPaidByChanged,
                )
                errorMessage?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            KipuDialogConfirmButton(
                text = if (isSaving) "Vinculando..." else "Vincular",
                onClick = onConfirm,
                enabled = !isSaving && movements.isNotEmpty(),
            )
        },
        dismissButton = {
            KipuDialogDismissButton(text = "Cancelar", onClick = onDismiss, enabled = !isSaving)
        },
    )
}

@Composable
private fun MovementOptionRow(
    movement: Movement,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RowWithRadio(
        selected = selected,
        onClick = onClick,
        label = "${MovementDisplayLabels.displayTitle(movement.counterpartyName, movement.description)} · " +
            formatPenAmountForDisplay(movement.amount.amount),
    )
}

@Composable
private fun ParticipantPicker(
    participants: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Text(text = "¿Quién pagó?", style = MaterialTheme.typography.labelLarge)
    participants.forEach { participant ->
        RowWithRadio(
            selected = participant.equals(selected, ignoreCase = true),
            onClick = { onSelected(participant) },
            label = participant,
        )
    }
}

@Composable
private fun RowWithRadio(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
