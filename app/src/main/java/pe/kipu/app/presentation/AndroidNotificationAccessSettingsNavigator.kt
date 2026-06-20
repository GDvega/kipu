package pe.kipu.app.presentation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import pe.kipu.core.domain.notification.NotificationAccessSettingsNavigator

class AndroidNotificationAccessSettingsNavigator @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationAccessSettingsNavigator {

    override fun openListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
