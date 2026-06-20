package pe.kipu.core.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.PaymentChannel

class MonitoredPaymentAppsTest {

    @Test
    fun verifiedPackagesMatchGooglePlayJun2026() {
        assertEquals("com.bcp.innovacxion.yapeapp", MonitoredPaymentApps.YAPE_PACKAGE)
        assertEquals("pe.com.interbank.mobilebanking", MonitoredPaymentApps.PLIN_PACKAGE)
    }

    @Test
    fun isMonitoredAcceptsVerifiedAndLegacyPackages() {
        assertTrue(MonitoredPaymentApps.isMonitored(MonitoredPaymentApps.YAPE_PACKAGE))
        assertTrue(MonitoredPaymentApps.isMonitored(MonitoredPaymentApps.PLIN_PACKAGE))
        assertTrue(MonitoredPaymentApps.isMonitored(MonitoredPaymentApps.LEGACY_YAPE_PACKAGE))
        assertTrue(MonitoredPaymentApps.isMonitored(MonitoredPaymentApps.LEGACY_PLIN_PACKAGE))
    }

    @Test
    fun isMonitoredRejectsUnknownPackages() {
        assertFalse(MonitoredPaymentApps.isMonitored("com.unknown.wallet"))
    }

    @Test
    fun verifiedPackagesAreMonitored() {
        MonitoredPaymentApps.verifiedPackages.forEach { packageName ->
            assertTrue(
                "Expected verified package to be monitored: $packageName",
                MonitoredPaymentApps.isMonitored(packageName),
            )
        }
    }

    @Test
    fun channelForPackageMapsToPaymentChannel() {
        assertEquals(PaymentChannel.YAPE, MonitoredPaymentApps.channelForPackage(MonitoredPaymentApps.YAPE_PACKAGE))
        assertEquals(PaymentChannel.PLIN, MonitoredPaymentApps.channelForPackage(MonitoredPaymentApps.PLIN_PACKAGE))
    }
}
