package pe.kipu.feature.profile.presentation

import pe.kipu.core.domain.export.ExportFormat
import pe.kipu.core.domain.model.ThemeMode

sealed interface ProfileEvent {
    data class ShareExport(
        val absolutePath: String,
        val mimeType: String,
    ) : ProfileEvent

    data object DataWiped : ProfileEvent
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val appVersionLabel: String = "Kipu 1.0",
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val notificationsEnabled: Boolean = false,
        val autoApproveHighConfidenceNotifications: Boolean = false,
        val notificationAccessGranted: Boolean = false,
        val showNotificationAccessDialog: Boolean = false,
        val onboardingCompleted: Boolean = false,
        val showExportWarningDialog: Boolean = false,
        val pendingExportFormat: ExportFormat? = null,
        val showWipeFirstConfirmDialog: Boolean = false,
        val showWipeFinalConfirmDialog: Boolean = false,
        val isExporting: Boolean = false,
        val isWiping: Boolean = false,
        val statusMessage: String? = null,
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}
