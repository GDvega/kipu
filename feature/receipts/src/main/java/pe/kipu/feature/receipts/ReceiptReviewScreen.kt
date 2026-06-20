package pe.kipu.feature.receipts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.KipuAlertCard
import pe.kipu.core.designsystem.component.KipuAlertTone
import pe.kipu.core.designsystem.component.KipuBadge
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPenOutlinedTextField
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuSectionHeader
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.feature.receipts.presentation.ReceiptCategorySuggestionTranslator
import pe.kipu.feature.receipts.presentation.ReceiptDuplicateDialog
import pe.kipu.feature.receipts.presentation.ReceiptReviewUiState
import pe.kipu.feature.receipts.presentation.ReceiptReviewViewModel
import pe.kipu.feature.receipts.presentation.receiptChannelLabel

@Composable
fun ReceiptReviewScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ReceiptReviewUiState.Saved || uiState is ReceiptReviewUiState.DuplicateMerged) {
            onFinished()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            ReceiptReviewUiState.Loading,
            is ReceiptReviewUiState.Processing,
            -> {
                KipuScreenHeader(
                    title = "Revisar comprobante",
                    subtitle = "Leyendo datos localmente",
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    KipuLoadingIndicator()
                }
            }

            is ReceiptReviewUiState.Ready -> {
                state.duplicatePending?.let { pending ->
                    ReceiptDuplicateDialog(
                        duplicatePending = pending,
                        onResolve = viewModel::onResolveDuplicate,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = KipuLayout.screenHorizontalPadding),
                ) {
                    KipuScreenHeader(
                        title = "Revisar comprobante",
                        subtitle = "Confirma antes de guardar",
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(KipuLayout.sectionSpacing),
                    ) {
                        ReceiptPreviewImage(bytes = state.previewBytes)

                        state.parseWarning?.let { warning ->
                            KipuAlertCard(tone = KipuAlertTone.Warning) {
                                Text(text = warning, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        state.confidence?.let { confidence ->
                            KipuBadge(
                                text = when (confidence) {
                                    SuggestionConfidence.HIGH -> "Lectura confiable"
                                    SuggestionConfidence.LOW -> "Revisa los campos"
                                },
                                tone = when (confidence) {
                                    SuggestionConfidence.HIGH -> KipuBadgeTone.Primary
                                    SuggestionConfidence.LOW -> KipuBadgeTone.Warning
                                },
                            )
                        }

                        Text(
                            text = "Canal: ${receiptChannelLabel(state.channel)}",
                            style = MaterialTheme.typography.labelLarge,
                        )

                        KipuPenOutlinedTextField(
                            value = state.amountText,
                            onValueChange = viewModel::onAmountChanged,
                            label = "Monto",
                        )

                        OutlinedTextField(
                            value = state.counterpartyText,
                            onValueChange = viewModel::onCounterpartyChanged,
                            label = { Text("Destinatario") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = state.operationReferenceText,
                            onValueChange = viewModel::onOperationReferenceChanged,
                            label = { Text("Nro. de operación (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = state.messageText,
                            onValueChange = viewModel::onMessageChanged,
                            label = { Text("Mensaje (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        KipuSectionHeader(
                            title = "Categoría",
                            horizontalPadding = 0.dp,
                        )

                        ReceiptCategorySuggestionTranslator.toDisplayText(state.categorySuggestionReason)?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        state.categories.forEach { category ->
                            val selected = category.id == state.selectedCategoryId
                            if (selected) {
                                KipuPrimaryButton(
                                    text = category.name,
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                KipuSecondaryButton(
                                    text = category.name,
                                    onClick = { viewModel.onCategorySelected(category.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isSaving,
                                )
                            }
                        }

                        state.errorMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        KipuPrimaryButton(
                            text = if (state.isSaving) "Guardando..." else "Guardar movimiento",
                            onClick = viewModel::onConfirm,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        KipuSecondaryButton(
                            text = "Cancelar",
                            onClick = onFinished,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            is ReceiptReviewUiState.Error -> {
                KipuScreenHeader(title = "Comprobante")
                KipuErrorState(
                    title = "No pudimos procesar el comprobante",
                    message = state.message,
                    retryLabel = "Volver",
                    onRetry = onFinished,
                )
            }

            is ReceiptReviewUiState.Saved,
            ReceiptReviewUiState.DuplicateMerged,
            -> Unit
        }
    }
}

@Composable
private fun ReceiptPreviewImage(
    bytes: ByteArray,
    modifier: Modifier = Modifier,
) {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    if (bitmap != null) {
        KipuCard(modifier = modifier.fillMaxWidth()) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Vista previa del comprobante",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
