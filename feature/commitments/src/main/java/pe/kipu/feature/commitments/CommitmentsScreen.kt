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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.kipuScrollbar
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLinearProgress
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
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
import pe.kipu.feature.commitments.ui.SavingsContributionDialog

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
                KipuScreenLoadingState()
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

                if (state.showContributionDialog) {
                    SavingsContributionDialog(
                        state = state.contributionState,
                        onIsDepositChanged = viewModel::onContributionIsDepositChanged,
                        onAmountChanged = viewModel::onContributionAmountChanged,
                        onPresetSelected = viewModel::onContributionPresetSelected,
                        onConfirm = viewModel::onConfirmContribution,
                        onDismiss = viewModel::onDismissContribution,
                    )
                }

                state.deleteTargetTitle?.let { title ->
                    CommitmentDeleteConfirmDialog(
                        title = title,
                        isDeleting = state.isDeleting,
                        errorMessage = state.deleteErrorMessage,
                        onConfirm = viewModel::onConfirmDelete,
                        onDismiss = viewModel::onDismissDelete,
                    )
                }

                val summaries = state.insights.summaries
                val invalidPlan =
                    state.insights.planValidation as? FinancialPlanValidationResult.Invalid

                val commitmentsListState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(
                    state = commitmentsListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .kipuScrollbar(commitmentsListState),
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
                            KipuEmptyState(
                                title = "Sin compromisos",
                                message = "Metas, deudas sociales y pagos pendientes aparecerán aquí.",
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
                                onContribute = { viewModel.onContributeClick(summary) },
                                onEdit = { viewModel.onEditClick(summary) },
                                onDelete = { viewModel.onDeleteClick(summary) },
                            )
                        }
                    }
                }
            }

            is CommitmentsUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar los compromisos",
                    message = state.message,
                    retryLabel = "Reintentar",
                    onRetry = viewModel::retryLoad,
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
            color = MaterialTheme.colorScheme.error,
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
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commitment = summary.commitment
    val statusText = CommitmentSummaryTranslator.statusText(summary)
    val amountLabel = CommitmentSummaryTranslator.amountLabel(summary)
    val progressText = CommitmentSummaryTranslator.savingsProgressText(summary)

    KipuCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(
                commitment.title,
                statusText,
                progressText,
            ).joinToString()
        },
    ) {
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
                commitment.savingsHorizonMonths?.let { months ->
                    Text(
                        text = "Plazo objetivo: $months meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                KipuLinearProgress(
                    progress = savingsProgress.progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                if (summary.isAtRisk) {
                    pe.kipu.core.designsystem.component.KipuBadge(
                        text = "⚠ El ahorro supera tu efectivo real",
                        tone = pe.kipu.core.designsystem.component.KipuBadgeTone.Critical,
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
                        color = MaterialTheme.colorScheme.error,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (commitment.type == CommitmentType.SAVINGS_GOAL) {
                    KipuPrimaryButton(
                        text = "+ Aportar / Retirar",
                        onClick = onContribute,
                        modifier = Modifier.weight(1.3f),
                    )
                }
                KipuSecondaryButton(
                    text = "Editar",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
                KipuSecondaryButton(
                    text = "Eliminar",
                    onClick = onDelete,
                    destructive = true,
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
            }
        }
    }
}

