package pe.kipu.feature.profile

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pe.kipu.core.designsystem.component.KipuAlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuDialogConfirmButton
import pe.kipu.core.designsystem.component.KipuDialogDismissButton
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuScreenLoadingState
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuTextLink
import pe.kipu.core.domain.export.ExportFormat
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.feature.profile.presentation.ProfileEvent
import pe.kipu.feature.profile.presentation.ProfileUiState
import pe.kipu.feature.profile.presentation.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToGatherings: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShareExport -> {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(event.absolutePath),
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Compartir exportación de Kipu"),
                    )
                }

                ProfileEvent.DataWiped -> Unit
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationAccessStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        KipuScreenHeader(title = "Perfil", subtitle = "Configuración y preferencias")
        when (val state = uiState) {
            ProfileUiState.Loading -> {
                KipuScreenLoadingState(
                    title = "Perfil",
                    subtitle = "Configuración y preferencias",
                )
            }

            is ProfileUiState.Content -> {
                if (state.showNotificationAccessDialog) {
                    NotificationAccessExplanationDialog(
                        onConfirm = viewModel::onNotificationAccessDialogConfirm,
                        onDismiss = viewModel::dismissNotificationAccessDialog,
                    )
                }
                if (state.showExportWarningDialog) {
                    ExportWarningDialog(
                        onConfirm = viewModel::confirmExportAfterWarning,
                        onDismiss = viewModel::dismissExportWarningDialog,
                    )
                }
                if (state.showWipeFirstConfirmDialog) {
                    WipeFirstConfirmDialog(
                        onConfirm = viewModel::confirmWipeFirstStep,
                        onDismiss = viewModel::dismissWipeFirstConfirmDialog,
                    )
                }
                if (state.showWipeFinalConfirmDialog) {
                    WipeFinalConfirmDialog(
                        onConfirm = viewModel::confirmWipeAllData,
                        onDismiss = viewModel::dismissWipeFinalConfirmDialog,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = KipuLayout.screenHorizontalPadding),
                ) {
                    KipuCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Apariencia",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Elige cómo se ve Kipu en tu dispositivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        KipuFilterChipRow(
                            labels = listOf("Claro", "Oscuro", "Sistema"),
                            selectedIndex = state.themeMode.toSelectorIndex(),
                            onSelected = { index ->
                                viewModel.setThemeMode(themeModeFromSelectorIndex(index))
                            },
                            contentPadding = PaddingValues(0.dp),
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        PreferenceSwitchRow(
                            title = "Notificaciones de ingresos",
                            subtitle = notificationSubtitle(
                                enabled = state.notificationsEnabled,
                                accessGranted = state.notificationAccessGranted,
                            ),
                            checked = state.notificationsEnabled,
                            onCheckedChange = viewModel::onNotificationsToggleChanged,
                        )
                        if (state.notificationsEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            PreferenceSwitchRow(
                                title = "Auto-registrar yapeos confiables",
                                subtitle = "Ingresos con número de operación se confirmarán automáticamente.",
                                checked = state.autoApproveHighConfidenceNotifications,
                                onCheckedChange = viewModel::onAutoApproveToggleChanged,
                            )
                        }
                    }

                    KipuCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KipuLayout.sectionSpacing),
                    ) {
                        Text(
                            text = "Cuentas compartidas",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Salidas, cenas y paseos con amigos. Registra quién participó para repartir gastos después.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        KipuSecondaryButton(
                            text = "Ver cuentas compartidas",
                            onClick = onNavigateToGatherings,
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                        )
                    }

                    KipuCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KipuLayout.sectionSpacing),
                    ) {
                        Text(
                            text = "Tus datos",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Exporta una copia local (JSON incluye cuentas compartidas; CSV solo movimientos). El CSV Excel usa punto y coma para abrir bien en Perú.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        KipuPrimaryButton(
                            text = if (state.isExporting) "Exportando..." else "Exportar JSON completo",
                            onClick = { viewModel.onExportRequested(ExportFormat.JSON) },
                            enabled = !state.isExporting && !state.isWiping,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        KipuSecondaryButton(
                            text = if (state.isExporting) "Exportando..." else "Exportar CSV de movimientos",
                            onClick = { viewModel.onExportRequested(ExportFormat.CSV) },
                            enabled = !state.isExporting && !state.isWiping,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            fillWidth = true,
                        )
                        KipuSecondaryButton(
                            text = if (state.isExporting) "Exportando..." else "Exportar CSV para Excel (Perú)",
                            onClick = { viewModel.onExportRequested(ExportFormat.CSV_EXCEL_PE) },
                            enabled = !state.isExporting && !state.isWiping,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            fillWidth = true,
                        )
                        KipuSecondaryButton(
                            text = if (state.isWiping) "Eliminando..." else "Eliminar todos mis datos",
                            onClick = viewModel::onWipeRequested,
                            enabled = !state.isExporting && !state.isWiping,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            fillWidth = true,
                        )
                    }

                    state.statusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    Text(
                        text = state.appVersionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    KipuTextLink(
                        text = "Política de privacidad",
                        onClick = onNavigateToPrivacyPolicy,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            is ProfileUiState.Error -> {
                KipuErrorState(
                    title = "No pudimos cargar el perfil",
                    message = state.message,
                    retryLabel = "Reintentar",
                    onRetry = viewModel::retryLoad,
                )
            }
        }
    }
}

@Composable
private fun ExportWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    KipuAlertDialog(
        title = "Archivo con datos sensibles",
        onDismissRequest = onDismiss,
        text = "La exportación incluye movimientos, sobres, compromisos y preferencias. " +
            "Guárdala en un lugar seguro y no la compartas por apps que no confíes.",
        confirmText = "Exportar",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun WipeFirstConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    KipuAlertDialog(
        title = "Eliminar todos los datos",
        onDismissRequest = onDismiss,
        text = "Se borrarán movimientos, duplicados descartados y preferencias. " +
            "Se restaurarán las categorías base para que puedas empezar de cero.",
        confirmText = "Continuar",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun WipeFinalConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    KipuAlertDialog(
        title = "¿Estás seguro?",
        onDismissRequest = onDismiss,
        text = "Esta acción no se puede deshacer. Volverás al inicio de la app " +
            "y tendrás que configurar Kipu otra vez.",
        confirmText = "Eliminar todo",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun NotificationAccessExplanationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    KipuAlertDialog(
        title = "Detectar ingresos de Yape y Plin",
        onDismissRequest = onDismiss,
        text = "Kipu no pide claves bancarias. Solo leemos notificaciones de ingreso en tu dispositivo " +
            "para sugerirte movimientos. Tú confirmas antes de guardarlos. " +
            "Puedes desactivarlo cuando quieras.\n\nEn la siguiente pantalla, activa el acceso para Kipu.",
        confirmText = "Ir a ajustes",
        onConfirm = onConfirm,
        dismissText = "Ahora no",
        onDismiss = onDismiss,
    )
}

private fun ThemeMode.toSelectorIndex(): Int = when (this) {
    ThemeMode.LIGHT -> 0
    ThemeMode.DARK -> 1
    ThemeMode.SYSTEM -> 2
}

private fun themeModeFromSelectorIndex(index: Int): ThemeMode = when (index) {
    0 -> ThemeMode.LIGHT
    1 -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

private fun notificationSubtitle(enabled: Boolean, accessGranted: Boolean): String = when {
    !enabled -> "Desactivado. Kipu no leerá notificaciones de pago."
    accessGranted -> "Activo · Acceso concedido"
    else -> "Activo · Acceso pendiente (abre ajustes del sistema)"
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
