package pe.kipu.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.app.receipt.ReceiptShareIntentParser

@RunWith(AndroidJUnit4::class)
class ReceiptShareIntentParserTest {

    @Test
    fun extractsImageUriFromSendIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "receipts/share-test.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(MINIMAL_JPEG)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        assertEquals(uri, ReceiptShareIntentParser.extractImageUri(intent))
    }

    @Test
    fun returnsNullForNonShareIntent() {
        val intent = Intent(Intent.ACTION_MAIN)
        assertNull(ReceiptShareIntentParser.extractImageUri(intent))
    }

    @Test
    fun returnsNullForNonImageMimeType() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
        }
        assertNull(ReceiptShareIntentParser.extractImageUri(intent))
    }

    @Test
    fun returnsNullForNonContentUri() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse("file:///tmp/receipt.jpg"))
        }

        assertNull(ReceiptShareIntentParser.extractImageUri(intent))
    }

    private companion object {
        val MINIMAL_JPEG: ByteArray = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte(),
        )
    }
}
