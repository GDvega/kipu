package pe.kipu.core.data.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationListenerBridgeTest {

    @Test
    fun `combines title and text with sanitized single line`() {
        val result = NotificationListenerBridge.combine("Yape", "MARIA te yapeó S/ 50.00")

        assertEquals("Yape MARIA te yapeó S/ 50.00", result)
    }

    @Test
    fun `returns blank when both parts are empty`() {
        val result = NotificationListenerBridge.combine(null, "   ")

        assertEquals("", result)
    }

    @Test
    fun `uses only title when text is null`() {
        val result = NotificationListenerBridge.combine("Plin", null)

        assertEquals("Plin", result)
    }
}
