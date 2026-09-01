package pe.kipu.core.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class FixedExpenseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val summary = intent.getStringExtra(EXTRA_SUMMARY)
            ?: "Recuerda revisar tus pagos fijos obligatorios de este ciclo."

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorios de pagos",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos al inicio de mes o quincena para pagos sí o sí"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio: Pagos sí o sí")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "kipu_fixed_payment_reminders"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_SUMMARY = "extra_fixed_payment_summary"
    }
}
