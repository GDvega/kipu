package pe.kipu.core.data.ocr

import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidReceiptImageLoaderTest {

    @Test
    fun `calculateInSampleSize halves dimensions until under max`() {
        val bounds = BitmapFactory.Options().apply {
            outWidth = 8192
            outHeight = 6144
        }

        val sampleSize = AndroidReceiptImageLoader.calculateInSampleSize(bounds, maxDimension = 2048)

        assertEquals(2, sampleSize)
    }

    @Test
    fun `calculateInSampleSize returns one when already small`() {
        val bounds = BitmapFactory.Options().apply {
            outWidth = 800
            outHeight = 600
        }

        val sampleSize = AndroidReceiptImageLoader.calculateInSampleSize(bounds, maxDimension = 2048)

        assertEquals(1, sampleSize)
    }
}
