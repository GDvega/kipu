package pe.kipu.core.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import pe.kipu.core.domain.notification.FixedExpenseReminderScheduler

@Singleton
class AndroidFixedExpenseReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : FixedExpenseReminderScheduler {

    override fun schedulePaymentReminders(itemsSummary: String, isBiweekly: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FixedExpenseReminderReceiver::class.java).apply {
            putExtra(FixedExpenseReminderReceiver.EXTRA_SUMMARY, itemsSummary)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = calculateNextReminderTime(isBiweekly)
        val interval = if (isBiweekly) 15L * 24 * 60 * 60 * 1000 else 30L * 24 * 60 * 60 * 1000

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                interval,
                pendingIntent,
            )
        } catch (_: SecurityException) {
            // In case exact alarm permission is restricted on specific OEM versions
        }
    }

    override fun cancelReminders() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FixedExpenseReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextReminderTime(isBiweekly: Boolean): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        if (isBiweekly) {
            if (currentDay < 15) {
                calendar.set(Calendar.DAY_OF_MONTH, 15)
            } else {
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        } else {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        return calendar.timeInMillis
    }

    private companion object {
        const val REQUEST_CODE = 3001
    }
}
