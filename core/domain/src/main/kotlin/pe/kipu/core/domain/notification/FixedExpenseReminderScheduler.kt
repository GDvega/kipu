package pe.kipu.core.domain.notification

interface FixedExpenseReminderScheduler {
    fun schedulePaymentReminders(itemsSummary: String, isBiweekly: Boolean)
    fun cancelReminders()
}
