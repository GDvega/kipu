package pe.kipu.app

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.app.receipt.ReceiptShareIntentParser

@RunWith(AndroidJUnit4::class)
class ReceiptShareIntentParserTest {

    @Test
    fun extractsImageUriFromSendIntent() {
        val uri = Uri.parse("content://external.receipts/share-test.jpg")

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
}
