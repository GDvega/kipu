package pe.kipu.core.data.notification

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.kipu.core.data.di.ApplicationScope
import pe.kipu.core.domain.model.NotificationParseResult
import pe.kipu.core.domain.notification.MonitoredPaymentApps
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.usecase.ParseNotificationTextUseCase
import pe.kipu.core.domain.usecase.RegisterNotificationIncomeUseCase

/**
 * Processes monitored payment notifications when user preference is enabled.
 * Does not log notification content.
 */
@Singleton
class NotificationListenerCoordinator @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val parseNotificationText: ParseNotificationTextUseCase,
    private val registerNotificationIncome: RegisterNotificationIncomeUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    fun onNotificationPosted(packageName: String, title: String?, text: String?) {
        if (!MonitoredPaymentApps.isMonitored(packageName)) return

        applicationScope.launch {
            processNotification(packageName, title, text)
        }
    }

    internal suspend fun processNotification(packageName: String, title: String?, text: String?) {
        val preferences = userPreferencesRepository.observePreferences().first()
        if (!preferences.notificationsEnabled) return

        val combined = NotificationListenerBridge.combine(title, text)
        if (combined.isBlank()) return

        val parseResult = parseNotificationText(packageName, combined)
        if (parseResult is NotificationParseResult.Success) {
            registerNotificationIncome(parseResult.suggestion)
        }
    }
}
