package pe.kipu.core.domain.notification

import pe.kipu.core.domain.model.PaymentChannel

/**
 * Allowlist of payment apps whose notifications Kipu may process for income detection.
 *
 * Package names verified against Google Play (jun 2026):
 * - Yape: [com.bcp.innovacxion.yapeapp]
 * - Plin (Interbank): [pe.com.interbank.mobilebanking] — Plin lives inside Interbank APP
 *
 * Legacy aliases ([com.bcp.yape], [pe.interbank.plin]) are kept for fixtures and older builds.
 */
object MonitoredPaymentApps {

    private val PACKAGE_TO_CHANNEL: Map<String, PaymentChannel> = mapOf(
        YAPE_PACKAGE to PaymentChannel.YAPE,
        LEGACY_YAPE_PACKAGE to PaymentChannel.YAPE,
        PLIN_PACKAGE to PaymentChannel.PLIN,
        LEGACY_PLIN_PACKAGE to PaymentChannel.PLIN,
    )

    /** Official Yape package on Google Play (Peru). */
    const val YAPE_PACKAGE: String = "com.bcp.innovacxion.yapeapp"

    /** Plin notifications arrive from the Interbank banking app (no standalone Plin APK). */
    const val PLIN_PACKAGE: String = "pe.com.interbank.mobilebanking"

    /** Pre-release assumption; kept for backward compatibility in tests. */
    const val LEGACY_YAPE_PACKAGE: String = "com.bcp.yape"

    /** Pre-release assumption; kept for backward compatibility in tests. */
    const val LEGACY_PLIN_PACKAGE: String = "pe.interbank.plin"

    val verifiedPackages: Set<String> = setOf(YAPE_PACKAGE, PLIN_PACKAGE)

    fun channelForPackage(packageName: String): PaymentChannel? = PACKAGE_TO_CHANNEL[packageName]

    fun isMonitored(packageName: String): Boolean = packageName in PACKAGE_TO_CHANNEL
}
