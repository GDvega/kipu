package pe.kipu.core.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.PaymentChannel

class MonitoredPaymentAppsTest {

    @Test
    fun verifiedPackagesMatchGooglePlay() {
        assertEquals("com.bcp.innovacxion.yapeapp", MonitoredPaymentApps.YAPE_PACKAGE)
        assertEquals("pe.com.interbank.mobilebanking", MonitoredPaymentApps.PLIN_PACKAGE)
    }

    @Test
    fun isMonitoredAcceptsVerifiedPackages() {
        assertTrue(MonitoredPaymentApps.isMonitored(MonitoredPaymentApps.YAPE_PACKAGE))
        assertTrue(MonitoredPaymentApps.isMonitored(MonitoredPaymentApps.PLIN_PACKAGE))
    }

    @Test
    fun isMonitoredRejectsUnknownOrLegacyPackages() {
        assertFalse(MonitoredPaymentApps.isMonitored("com.unknown.wallet"))
        assertFalse(MonitoredPaymentApps.isMonitored("com.bcp.yape"))
        assertFalse(MonitoredPaymentApps.isMonitored("pe.interbank.plin"))
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
