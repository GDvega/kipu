package pe.kipu.feature.commitments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLinearProgress
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuSectionHeader
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.theme.KipuExpense
import pe.kipu.core.designsystem.theme.KipuSecondary
import pe.kipu.core.domain.model.CommitmentSummary
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.feature.commitments.presentation.CommitmentSummaryTranslator
import pe.kipu.feature.commitments.presentation.CommitmentsUiState
import pe.kipu.feature.commitments.presentation.CommitmentsViewModel
import pe.kipu.feature.commitments.ui.CommitmentDeleteConfirmDialog
import pe.kipu.feature.commitments.ui.CommitmentFormDialog

@Composable
fun CommitmentsScreen(
    modifier: Modifier = Modifier,
    viewModel: CommitmentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        KipuScreenHeader(title = "Compromisos")
        when (val state = uiState) {
            CommitmentsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    KipuLoadingIndicator()
                }
            }

            is CommitmentsUiState.Content -> {
                if (state.showFormDialog) {
                    CommitmentFormDialog(
                        formState = state.formState,
                        onTypeSelected = viewModel::onTypeSelected,
                        onTitleChanged = viewModel::onTitleChanged,
                        onTargetAmountChanged = viewModel::onTargetAmountChanged,
                        onCurrentAmountChanged = viewModel::onCurrentAmountChanged,
                        onCounterpartyChanged = viewModel::onCounterpartyChanged,
                        onConfirm = viewModel::onConfirmForm,
                        onDismiss = viewModel::onDismissForm,
                    )
                }
                if (state.deleteTargetId != null && state.deleteTargetTitle != null) {
                    CommitmentDeleteConfirmDialog(
                        title = state.deleteTargetTitle,
                        onConfirm = viewModel::onConfirmDelete,
                        onDismiss = viewModel::onDismissDelete,
                    )
                }

                val summaries = state.insights.summaries
                val invalidPlan =
                    state.insights.planValidation as? FinancialPlanValidationResult.Invalid

                if (summaries.isEmpty() && invalidPlan == null) {
                    KipuEmptyState(
                        title = "Sin compromisos",
                        message = "Metas, deudas sociales y pagos pendientes aparecerán aquí.",
                        actionLabel = "Nuevo compromiso",
                        onAction = viewModel::onCreateClick,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KipuLayout.listItemSpacing),
                        contentPadding = KipuLayout.screenContentPadding(),
                    ) {
                        item(key = "add-commitment") {
                            KipuPrimaryButton(
                                text = "Nuevo compromiso",
                                onClick = viewModel::onCreateClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        invalidPlan?.let { validation ->
                            item(key = "plan-alert") {
                                PlanValidationAlertCard(validation = validation)
                            }
                        }
                        if (summaries.isEmpty()) {
                            item(key = "empty-commitments-hint") {
                                Text(
                                    text = "Aún no tienes compromisos registrados.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            item(key = "header") {
                                KipuSectionHeader(
                                    title = "Tus compromisos",
                                    horizontalPadding = 0.dp,
                                )
                            }
                            items(
                                items = summaries,
                                key = { summary -> summary.commitment.id },
                            ) { summary ->
                                CommitmentListItem(
                                    summary = summary,
                                    onEdit = { viewModel.onEditClick(summary) },
                                    onDelete = { viewModel.onDeleteClick(summary) },
                                )
                            }
                        }
                    }
                }
            }

            is CommitmentsUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar los compromisos",
                    message = state.message,
                )
            }
        }
    }
}

@Composable
private fun PlanValidationAlertCard(
    validation: FinancialPlanValidationResult.Invalid,
    modifier: Modifier = Modifier,
) {
    val message = CommitmentSummaryTranslator.planValidationText(validation) ?: return

    KipuCard(modifier = modifier) {
        Text(
            text = "Plan financiero en negativo",
            style = MaterialTheme.typography.titleMedium,
            color = KipuExpense,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CommitmentListItem(
    summary: CommitmentSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commitment = summary.commitment
    val statusText = CommitmentSummaryTranslator.statusText(summary)
    val amountLabel = CommitmentSummaryTranslator.amountLabel(summary)
    val progressText = CommitmentSummaryTranslator.savingsProgressText(summary)

    KipuCard(modifier = modifier) {
        Column {
            Text(
                text = commitment.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            val savingsProgress = summary.savingsProgress
            if (commitment.type == CommitmentType.SAVINGS_GOAL && savingsProgress != null) {
                KipuLinearProgress(
                    progress = savingsProgress.progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    fillColor = MaterialTheme.colorScheme.primary,
                    trackColor = KipuSecondary.copy(alpha = 0.3f),
                )
                progressText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else if (amountLabel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Monto pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = amountLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = KipuExpense,
                    )
                }
            }
            commitment.counterpartyName?.takeIf { it.isNotBlank() }?.let { name ->
                Text(
                    text = "Contraparte: $name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KipuSecondaryButton(
                    text = "Editar",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
                KipuSecondaryButton(
                    text = "Eliminar",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
            }
        }
    }
}
