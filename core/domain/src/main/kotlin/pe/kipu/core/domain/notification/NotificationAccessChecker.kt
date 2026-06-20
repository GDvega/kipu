package pe.kipu.core.domain.notification

/**
 * Checks whether Kipu has been granted notification listener access by the user.
 */
fun interface NotificationAccessChecker {
    fun isAccessGranted(): Boolean
}
