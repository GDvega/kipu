package pe.kipu.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAlertCard
import pe.kipu.core.designsystem.component.KipuAlertTone
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuBadge
import pe.kipu.core.designsystem.component.KipuBadgeTone
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuHeroCard
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuLoadingIndicator
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSectionHeader
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.DailyAvailableBudget
import pe.kipu.feature.home.presentation.HomeAlertTranslator
import pe.kipu.feature.home.presentation.HomeUiState
import pe.kipu.feature.home.presentation.HomeViewModel

@Composable
fun HomeScreen(
    onRegisterReceipt: () -> Unit = {},
    onRegisterCash: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    KipuLoadingIndicator()
                }
            }

            is HomeUiState.Content -> {
                val insights = state.insights
                if (insights.movementCount == 0 && insights.envelopeCount == 0) {
                    KipuScreenHeader(
                        title = "Tu dinero protegido",
                        greeting = "Hola",
                    )
                    KipuEmptyState(
                        title = "Bienvenido a Kipu",
                        message = "Aún no hay movimientos ni sobres. Empieza registrando tu primer gasto.",
                        actionLabel = "Registrar movimiento",
                        onAction = onRegisterCash,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KipuLayout.sectionSpacing),
                        contentPadding = PaddingValues(bottom = KipuLayout.screenHorizontalPadding),
                    ) {
                        item {
                            KipuScreenHeader(
                                title = "Tu dinero protegido",
                                greeting = "Hola",
                            )
                        }
                        item {
                            DailyAvailableCard(
                                dailyAvailable = insights.dailyAvailable,
                                envelopeCount = insights.envelopeCount,
                                modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                            )
                        }
                        item {
                            KipuCard(modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding)) {
                                Text(
                                    text = "¿Pagaste con Yape, Plin o efectivo?",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "Comparte un comprobante o registra un gasto en efectivo al instante.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                KipuPrimaryButton(
                                    text = "Registrar comprobante",
                                    onClick = onRegisterReceipt,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                )
                                KipuSecondaryButton(
                                    text = "Registrar en efectivo",
                                    onClick = onRegisterCash,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    fillWidth = true,
                                )
                            }
                        }
                        if (insights.antSpendingAlerts.isNotEmpty()) {
                            item {
                                KipuSectionHeader(title = "Gastos hormiga")
                            }
                            items(
                                items = insights.antSpendingAlerts,
                                key = { alert ->
                                    "${alert.categoryId}-${alert.transactionCount}-${alert.totalAmount.amount}"
                                },
                            ) { alert ->
                                AntSpendingAlertCard(
                                    alert = alert,
                                    categoryName = state.categoryNamesById[alert.categoryId],
                                    modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
                                )
                            }
                        }
                    }
                }
            }

            is HomeUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar el inicio",
                    message = state.message,
                )
            }
        }
    }
}

@Composable
private fun DailyAvailableCard(
    dailyAvailable: DailyAvailableBudget,
    envelopeCount: Int,
    modifier: Modifier = Modifier,
) {
    val availableAmount = dailyAvailable.dailyAvailable
    KipuHeroCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when {
                    envelopeCount == 0 -> "Configura sobres para ver tu disponible"
                    dailyAvailable.isOverBudget -> "Presupuesto semanal excedido"
                    availableAmount != null ->
                        "Disponible hoy: S/ ${availableAmount.amount.toPlainString()}"
                    else -> "Disponible hoy no calculado"
                }
            },
    ) {
        Text(
            text = "DISPONIBLE HOY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        when {
            envelopeCount == 0 -> {
                Text(
                    text = "Configura sobres para ver tu disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            dailyAvailable.isOverBudget -> {
                KipuBadge(
                    text = "Presupuesto excedido",
                    tone = KipuBadgeTone.Critical,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "Ya pasaste tu presupuesto semanal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            dailyAvailable.daysRemainingInWeek <= 0 -> {
                Text(
                    text = "Sin días restantes esta semana",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            else -> {
                dailyAvailable.dailyAvailable?.let { amount ->
                    KipuAmountText(
                        amount = amount.amount,
                        type = AmountType.INCOME,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    KipuBadge(
                        text = "Te quedan ${dailyAvailable.daysRemainingInWeek} días esta semana",
                        tone = KipuBadgeTone.Primary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AntSpendingAlertCard(
    alert: AntSpendingAlert,
    categoryName: String?,
    modifier: Modifier = Modifier,
) {
    val tone = when (alert.severity) {
        AlertSeverity.AMBER -> KipuAlertTone.Warning
        AlertSeverity.RED -> KipuAlertTone.Critical
    }
    val badgeTone = when (alert.severity) {
        AlertSeverity.AMBER -> KipuBadgeTone.Warning
        AlertSeverity.RED -> KipuBadgeTone.Critical
    }
    val titleColor = when (alert.severity) {
        AlertSeverity.AMBER -> MaterialTheme.colorScheme.secondary
        AlertSeverity.RED -> KipuRed
    }

    KipuAlertCard(tone = tone, modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Gasto hormiga detectado",
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor,
        )
        Text(
            text = HomeAlertTranslator.toDisplayText(alert, categoryName),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        KipuBadge(
            text = "${alert.transactionCount} movimientos en 48 h",
            tone = badgeTone,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
