package pe.kipu.core.data.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import pe.kipu.core.domain.notification.MonitoredPaymentApps

/**
 * Listens for Yape and Plin notifications only. Respects user preference via [NotificationListenerCoordinator].
 * Never logs notification title, body, amounts or names.
 */
@AndroidEntryPoint
class KipuNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var coordinator: NotificationListenerCoordinator

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!MonitoredPaymentApps.isMonitored(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = extractNotificationBody(extras)

        coordinator.onNotificationPosted(packageName, title, text)
    }

    private fun extractNotificationBody(extras: android.os.Bundle): String? {
        val keys = listOf(
            android.app.Notification.EXTRA_TEXT,
            android.app.Notification.EXTRA_BIG_TEXT,
            "android.text",
        )
        return keys.firstNotNullOfOrNull { key ->
            extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }
        }
    }
}
