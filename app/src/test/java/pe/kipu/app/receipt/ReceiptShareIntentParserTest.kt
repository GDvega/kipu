package pe.kipu.app.receipt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReceiptShareIntentParserTest {

    @Test
    fun isSupportedMimeType_validImageTypes_returnsTrue() {
        assertTrue(ReceiptShareIntentParser.isSupportedMimeType("image/jpeg"))
        assertTrue(ReceiptShareIntentParser.isSupportedMimeType("image/jpg"))
        assertTrue(ReceiptShareIntentParser.isSupportedMimeType("image/png"))
        assertTrue(ReceiptShareIntentParser.isSupportedMimeType("image/webp"))
        assertTrue(ReceiptShareIntentParser.isSupportedMimeType("IMAGE/JPEG"))
        assertTrue(ReceiptShareIntentParser.isSupportedMimeType("image/*"))
    }

    @Test
    fun isSupportedMimeType_unsupportedOrMaliciousTypes_returnsFalse() {
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("image/svg+xml"))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("image/gif"))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("image/vnd.wap.wbmp"))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("text/plain"))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("application/pdf"))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("application/octet-stream"))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType(""))
        assertFalse(ReceiptShareIntentParser.isSupportedMimeType("   "))
    }

    @Test
    fun isSafeContentUri_validContentUri_returnsTrue() {
        assertTrue(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "content",
                authority = "com.google.android.apps.photos.contentprovider",
                path = "/media/123",
            ),
        )
    }

    @Test
    fun isSafeContentUri_nonContentScheme_returnsFalse() {
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "file",
                authority = null,
                path = "/sdcard/test.jpg",
            ),
        )
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "http",
                authority = "evil.com",
                path = "/malicious.jpg",
            ),
        )
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "https",
                authority = "evil.com",
                path = "/malicious.jpg",
            ),
        )
    }

    @Test
    fun isSafeContentUri_missingAuthorityOrPath_returnsFalse() {
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "content",
                authority = "",
                path = "/test",
            ),
        )
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "content",
                authority = null,
                path = "/test",
            ),
        )
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "content",
                authority = "media",
                path = "",
            ),
        )
        assertFalse(
            ReceiptShareIntentParser.isSafeContentUri(
                scheme = "content",
                authority = "media",
                path = null,
            ),
        )
    }
}
