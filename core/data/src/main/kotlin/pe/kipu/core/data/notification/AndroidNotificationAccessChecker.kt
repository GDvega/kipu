package pe.kipu.core.data.notification

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import pe.kipu.core.domain.notification.NotificationAccessChecker

class AndroidNotificationAccessChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationAccessChecker {

    override fun isAccessGranted(): Boolean {
        val enabledPackages = Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS,
        ) ?: return false
        return enabledPackages.split(':').any { component ->
            component.startsWith("${context.packageName}/")
        }
    }

    companion object {
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    }
}
