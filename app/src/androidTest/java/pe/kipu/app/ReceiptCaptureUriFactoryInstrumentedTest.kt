package pe.kipu.app

import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.feature.receipts.ReceiptCaptureUriFactory

@RunWith(AndroidJUnit4::class)
class ReceiptCaptureUriFactoryInstrumentedTest {

    @Test
    fun deletesOnlyTheCaptureFileCreatedByKipu() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val captureUri = ReceiptCaptureUriFactory.create(context)
        val captureFile = File(context.cacheDir, "receipts/${captureUri.lastPathSegment}")
        val importedFile = File(context.cacheDir, "receipts/imported-receipt.jpg")
        captureFile.writeBytes(MINIMAL_JPEG)
        importedFile.writeBytes(MINIMAL_JPEG)
        val importedUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            importedFile,
        )

        try {
            assertTrue(ReceiptCaptureUriFactory.deleteIfOwnedCapture(context, captureUri.toString()))
            assertFalse(captureFile.exists())

            assertFalse(ReceiptCaptureUriFactory.deleteIfOwnedCapture(context, importedUri.toString()))
            assertTrue(importedFile.exists())

            val wrongSchemeUri = "file://${context.packageName}.fileprovider${captureUri.path}"
            captureFile.writeBytes(MINIMAL_JPEG)
            assertFalse(ReceiptCaptureUriFactory.deleteIfOwnedCapture(context, wrongSchemeUri))
            assertTrue(captureFile.exists())
        } finally {
            captureFile.delete()
            importedFile.delete()
        }
    }

    private companion object {
        val MINIMAL_JPEG: ByteArray = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte(),
        )
    }
}
