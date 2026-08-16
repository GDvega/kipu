package pe.kipu.feature.juntas

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
import pe.kipu.core.designsystem.component.KipuSubScreenScaffold
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.GatheringSummary
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.util.MovementDisplayLabels
import pe.kipu.feature.juntas.presentation.GatheringDialogMode
import pe.kipu.feature.juntas.presentation.GatheringsUiState
import pe.kipu.feature.juntas.presentation.GatheringsViewModel

@Composable
fun GatheringsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GatheringsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    KipuSubScreenScaffold(
        title = "Cuentas compartidas",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Gastos compartidos con amigos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = KipuLayout.screenHorizontalPadding,
                    vertical = 8.dp,
                ),
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
                state.deleteTarget?.let { target ->
                    GatheringDeleteConfirmDialog(
                        gatheringName = target.gathering.name,
                        isDeleting = state.isDeleting,
                        errorMessage = state.deleteErrorMessage,
                        onConfirm = viewModel::onConfirmDelete,
                        onDismiss = viewModel::onDismissDelete,
                    )
                }
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
                        title = if (mode is GatheringDialogMode.Edit) "Editar cuenta" else "Nueva cuenta",
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
                        title = "Sin cuentas",
                        message = "Registra salidas, cenas o paseos para repartir gastos después.",
                        icon = null,
                        actionLabel = "Nueva cuenta",
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
                                text = "Nueva cuenta",
                                onClick = viewModel::onCreateClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        itemsIndexed(items = state.summaries, key = { _, it -> it.gathering.id }) { index, summary ->
                            pe.kipu.core.designsystem.component.KipuAnimatedListItem(index = index) {
                                GatheringCard(
                                    summary = summary,
                                    onEdit = { viewModel.onEditClick(summary) },
                                    onRecordExpense = { viewModel.onRecordExpenseClick(summary.gathering.id) },
                                    onLinkMovement = { viewModel.onLinkMovementClick(summary.gathering.id) },
                                    onDelete = { viewModel.onDeleteClick(summary) },
                                )
                            }
                        }
                    }
                }
            }

            is GatheringsUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar tus cuentas compartidas",
                    message = state.message,
                    retryLabel = "Reintentar",
                    onRetry = viewModel::retryLoad,
                )
            }
            }
        }
    }
}

@Composable
private fun GatheringDeleteConfirmDialog(
    gatheringName: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(text = "Eliminar cuenta compartida") },
        text = {
            Column {
                Text(
                    text = "¿Eliminar \"$gatheringName\"? También se borrarán todos sus gastos compartidos. Esta acción no se puede deshacer.",
                )
                errorMessage?.let { message -> GatheringFormErrorText(message) }
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
    var expandedMenu by remember { mutableStateOf(false) }

    KipuCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = gathering.name, style = MaterialTheme.typography.titleMedium)
                
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    gathering.participantNames.take(4).forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f + (index * 0.1f).coerceAtMost(0.8f))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (gathering.participantNames.size > 4) {
                        Text(text = "+${gathering.participantNames.size - 4}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                Text(
                    text = if (gathering.participantNames.size == 1) {
                        "1 participante"
                    } else {
                        "${gathering.participantNames.size} participantes"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Opciones de ${gathering.name}",
                    )
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Registrar gasto") },
                        onClick = { expandedMenu = false; onRecordExpense() }
                    )
                    DropdownMenuItem(
                        text = { Text("Vincular movimiento") },
                        onClick = { expandedMenu = false; onLinkMovement() }
                    )
                    DropdownMenuItem(
                        text = { Text("Editar cuenta") },
                        onClick = { expandedMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar cuenta") },
                        onClick = { expandedMenu = false; onDelete() }
                    )
                }
            }
        }

        if (!summary.totalExpenses.isZero()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Total gastado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            pe.kipu.core.designsystem.component.KipuAmountText(
                amount = summary.totalExpenses.amount,
                type = pe.kipu.core.designsystem.component.AmountType.EXPENSE,
            )
            
            Text(
                text = "Cuota por persona: ${formatPenAmountForDisplay(summary.perPersonAmount.amount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            
            if (summary.settlements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Balances:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                summary.settlements.forEach { settlement ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = settlement.participantName,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = if (settlement.balanceDirection == pe.kipu.core.domain.model.SettlementDirection.OWES) "debe" else "recibe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatPenAmountForDisplay(settlement.balanceAmount.amount),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settlement.balanceDirection == pe.kipu.core.domain.model.SettlementDirection.OWES) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        } else {
             Text(
                text = "Aún no hay gastos registrados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
             )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val currentIsSaving = rememberUpdatedState(isSaving)
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = remember {
            { target -> target != SheetValue.Hidden || !currentIsSaving.value }
        },
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true,
            )
            OutlinedTextField(
                value = participants,
                onValueChange = onParticipantsChanged,
                label = { Text("Participantes") },
                placeholder = { Text("Uno por línea o separados por coma") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                enabled = !isSaving,
            )
            errorMessage?.let { message ->
                GatheringFormErrorText(message)
            }
            KipuPrimaryButton(
                text = if (isSaving) "Guardando..." else "Guardar",
                onClick = onConfirm,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fillWidth = true,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val currentIsSaving = rememberUpdatedState(isSaving)
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        confirmValueChange = remember {
            { target -> target != SheetValue.Hidden || !currentIsSaving.value }
        },
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Registrar gasto", style = MaterialTheme.typography.titleLarge)
            KipuPenOutlinedTextField(
                value = amount,
                onValueChange = onAmountChanged,
                label = "Monto",
                enabled = !isSaving,
            )
            ParticipantPicker(
                participants = participants,
                selected = paidBy,
                onSelected = onPaidByChanged,
                enabled = !isSaving,
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChanged,
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true,
            )
            errorMessage?.let { message ->
                GatheringFormErrorText(message)
            }
            KipuPrimaryButton(
                text = if (isSaving) "Guardando..." else "Guardar",
                onClick = onConfirm,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fillWidth = true,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LinkMovementDialog(
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
    val currentIsSaving = rememberUpdatedState(isSaving)
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        confirmValueChange = remember {
            { target -> target != SheetValue.Hidden || !currentIsSaving.value }
        },
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Vincular movimiento", style = MaterialTheme.typography.titleLarge)
            if (movements.isEmpty()) {
                Text(
                    text = "No hay movimientos de gasto confirmados sin vincular.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items = movements, key = { movement -> movement.id }) { movement ->
                        MovementOptionRow(
                            movement = movement,
                            selected = movement.id == selectedMovementId,
                            onClick = { onMovementSelected(movement.id) },
                            enabled = !isSaving,
                        )
                    }
                }
            }
            ParticipantPicker(
                participants = participants,
                selected = paidBy,
                onSelected = onPaidByChanged,
                enabled = !isSaving,
            )
            errorMessage?.let { message ->
                GatheringFormErrorText(message)
            }
            KipuPrimaryButton(
                text = if (isSaving) "Vinculando..." else "Vincular",
                onClick = onConfirm,
                enabled = !isSaving && movements.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun MovementOptionRow(
    movement: Movement,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    RowWithRadio(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = "${MovementDisplayLabels.displayTitle(movement.counterpartyName, movement.description)} · " +
            formatPenAmountForDisplay(movement.amount.amount),
    )
}

@Composable
private fun ParticipantPicker(
    participants: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    enabled: Boolean,
) {
    Text(text = "¿Quién pagó?", style = MaterialTheme.typography.labelLarge)
    Column(modifier = Modifier.selectableGroup()) {
        participants.forEach { participant ->
            RowWithRadio(
                selected = participant.equals(selected, ignoreCase = true),
                onClick = { onSelected(participant) },
                label = participant,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun RowWithRadio(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    enabled: Boolean,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
    }
}

@Composable
private fun GatheringFormErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics {
            error(message)
            liveRegion = LiveRegionMode.Polite
        },
    )
}
