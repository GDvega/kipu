package pe.kipu.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.export.ExportFormat
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.notification.NotificationAccessChecker
import pe.kipu.core.domain.notification.NotificationAccessSettingsNavigator
import pe.kipu.core.domain.repository.UserDataExportFileRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.usecase.ExportUserDataUseCase
import pe.kipu.core.domain.usecase.WipeAllUserDataUseCase

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationAccessChecker: NotificationAccessChecker,
    private val notificationAccessSettingsNavigator: NotificationAccessSettingsNavigator,
    private val exportUserData: ExportUserDataUseCase,
    private val exportFileRepository: UserDataExportFileRepository,
    private val wipeAllUserData: WipeAllUserDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest {
                    userPreferencesRepository.observePreferences()
                        .map { preferences ->
                            val accessGranted = notificationAccessChecker.isAccessGranted()
                            val current = _uiState.value
                            val previous = current as? ProfileUiState.Content
                            val dialogVisible = previous?.showNotificationAccessDialog == true &&
                                !accessGranted &&
                                preferences.notificationsEnabled
                            ProfileUiState.Content(
                                themeMode = preferences.themeMode,
                                notificationsEnabled = preferences.notificationsEnabled,
                                notificationAccessGranted = accessGranted,
                                showNotificationAccessDialog = dialogVisible,
                                onboardingCompleted = preferences.onboardingCompleted,
                                showExportWarningDialog = previous?.showExportWarningDialog == true,
                                pendingExportFormat = previous?.pendingExportFormat,
                                showWipeFirstConfirmDialog = previous?.showWipeFirstConfirmDialog == true,
                                showWipeFinalConfirmDialog = previous?.showWipeFinalConfirmDialog == true,
                                isExporting = previous?.isExporting == true,
                                isWiping = previous?.isWiping == true,
                                status = previous?.status,
                            )
                        }
                        .map<ProfileUiState.Content, ProfileUiState> { it }
                        .catch {
                            emit(ProfileUiState.Error("No pudimos cargar tus preferencias"))
                        }
                }.collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    fun refreshNotificationAccessStatus() {
        val accessGranted = notificationAccessChecker.isAccessGranted()
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(
                    notificationAccessGranted = accessGranted,
                    showNotificationAccessDialog = current.showNotificationAccessDialog &&
                        !accessGranted &&
                        current.notificationsEnabled,
                )
            } else {
                current
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        updatePreferences { current ->
            current.copy(themeMode = mode)
        }
    }

    fun onNotificationsToggleChanged(enabled: Boolean) {
        if (enabled) {
            updatePreferences { current -> current.copy(notificationsEnabled = true) }
            if (!notificationAccessChecker.isAccessGranted()) {
                _uiState.update { current ->
                    if (current is ProfileUiState.Content) {
                        current.copy(showNotificationAccessDialog = true)
                    } else {
                        current
                    }
                }
            }
        } else {
            updatePreferences { current -> current.copy(notificationsEnabled = false) }
            dismissNotificationAccessDialog()
        }
    }

    fun onNotificationAccessDialogConfirm() {
        notificationAccessSettingsNavigator.openListenerSettings()
        dismissNotificationAccessDialog()
    }

    fun dismissNotificationAccessDialog() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(showNotificationAccessDialog = false)
            } else {
                current
            }
        }
    }

    fun onExportRequested(format: ExportFormat) {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(
                    showExportWarningDialog = true,
                    pendingExportFormat = format,
                    status = null,
                )
            } else {
                current
            }
        }
    }

    fun dismissExportWarningDialog() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(showExportWarningDialog = false, pendingExportFormat = null)
            } else {
                current
            }
        }
    }

    fun confirmExportAfterWarning() {
        val state = _uiState.value as? ProfileUiState.Content ?: return
        if (state.isExporting || state.isWiping || !state.showExportWarningDialog) return
        val format = state.pendingExportFormat ?: return
        _uiState.value = state.copy(
            isExporting = true,
            showExportWarningDialog = false,
            pendingExportFormat = null,
            status = null,
        )
        viewModelScope.launch {
            exportUserData(format)
                .mapCatching { payload ->
                    exportFileRepository.writeExport(
                        content = payload.content,
                        fileName = payload.fileName,
                        mimeType = payload.format.mimeType,
                    ).getOrThrow()
                }
                .onSuccess { stored ->
                    _events.emit(
                        ProfileEvent.ShareExport(
                            absolutePath = stored.absolutePath,
                            mimeType = stored.mimeType,
                        ),
                    )
                    setStatus(ProfileStatus.Success("Exportación lista para compartir."))
                }
                .onFailure {
                    setStatus(ProfileStatus.Error("No pudimos exportar tus datos. Intenta de nuevo."))
                }
            _uiState.update { current ->
                (current as? ProfileUiState.Content)?.copy(isExporting = false) ?: current
            }
        }
    }

    fun onWipeRequested() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(showWipeFirstConfirmDialog = true, status = null)
            } else {
                current
            }
        }
    }

    fun dismissWipeFirstConfirmDialog() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(showWipeFirstConfirmDialog = false)
            } else {
                current
            }
        }
    }

    fun confirmWipeFirstStep() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(
                    showWipeFirstConfirmDialog = false,
                    showWipeFinalConfirmDialog = true,
                )
            } else {
                current
            }
        }
    }

    fun dismissWipeFinalConfirmDialog() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(showWipeFinalConfirmDialog = false)
            } else {
                current
            }
        }
    }

    fun confirmWipeAllData() {
        val state = _uiState.value as? ProfileUiState.Content ?: return
        if (state.isExporting || state.isWiping || !state.showWipeFinalConfirmDialog) return
        _uiState.value = state.copy(
            isWiping = true,
            showWipeFinalConfirmDialog = false,
            status = null,
        )
        viewModelScope.launch {
            wipeAllUserData()
                .onSuccess {
                    _events.emit(ProfileEvent.DataWiped)
                }
                .onFailure {
                    setStatus(ProfileStatus.Error("No pudimos eliminar tus datos. Intenta de nuevo."))
                }
            _uiState.update { current ->
                (current as? ProfileUiState.Content)?.copy(isWiping = false) ?: current
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(status = null)
            } else {
                current
            }
        }
    }

    private fun setStatus(status: ProfileStatus) {
        _uiState.update { current ->
            if (current is ProfileUiState.Content) {
                current.copy(status = status)
            } else {
                current
            }
        }
    }

    private fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(transform)
                .onFailure {
                    setStatus(ProfileStatus.Error("No pudimos guardar tus preferencias. Intenta de nuevo."))
                }
        }
    }
}
