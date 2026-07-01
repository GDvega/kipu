package pe.kipu.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.repository.UserPreferencesRepository

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val pendingOpenManualMovement = MutableStateFlow(false)
    private val _pendingReceiptUri = MutableStateFlow<String?>(null)
    val pendingReceiptUri: StateFlow<String?> = _pendingReceiptUri.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.observePreferences()
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )

    val onboardingCompleted: StateFlow<Boolean> = userPreferencesRepository.observePreferences()
        .map { it.onboardingCompleted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val pendingPlanWizard: StateFlow<Boolean> = userPreferencesRepository.observePreferences()
        .map { it.pendingPlanWizard }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun clearPendingPlanWizard() {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { preferences ->
                preferences.copy(pendingPlanWizard = false)
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { preferences ->
                preferences.copy(onboardingCompleted = false, pendingPlanWizard = false)
            }
        }
    }

    fun markPendingOpenManualMovement() {
        pendingOpenManualMovement.value = true
    }

    fun consumePendingOpenManualMovement(): Boolean {
        if (!pendingOpenManualMovement.value) return false
        pendingOpenManualMovement.value = false
        return true
    }

    fun onSharedReceiptUri(uri: String) {
        _pendingReceiptUri.value = uri
    }

    fun consumePendingReceiptUri(): String? {
        val uri = _pendingReceiptUri.value
        _pendingReceiptUri.value = null
        return uri
    }
}
