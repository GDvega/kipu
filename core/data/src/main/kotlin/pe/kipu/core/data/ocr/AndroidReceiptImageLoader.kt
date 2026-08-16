package pe.kipu.core.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.ocr.ReceiptImageLoader

@Singleton
class AndroidReceiptImageLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReceiptImageLoader {

    override suspend fun load(contentUri: String): Result<OcrImage> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = contentUri.toUri()
            val bounds = decodeBounds(uri)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                error("Invalid receipt image")
            }

            var bitmap = decodeSampledBitmap(uri, bounds)
                ?: error("Invalid receipt image")

            var jpegBytes = bitmap.toJpegBytes()
            var scaleAttempts = 0
            while (jpegBytes.size > OcrImage.MAX_BYTES && scaleAttempts < MAX_SCALE_ATTEMPTS) {
                val scaledWidth = (bitmap.width * SCALE_FACTOR).toInt().coerceAtLeast(1)
                val scaledHeight = (bitmap.height * SCALE_FACTOR).toInt().coerceAtLeast(1)
                bitmap = bitmap.scale(scaledWidth, scaledHeight, true)
                jpegBytes = bitmap.toJpegBytes()
                scaleAttempts++
            }

            if (jpegBytes.size > OcrImage.MAX_BYTES) {
                error("Receipt image exceeds maximum allowed size")
            }

            OcrImage(
                bytes = jpegBytes,
                width = bitmap.width,
                height = bitmap.height,
            )
        }
    }

    private fun decodeBounds(uri: Uri): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: error("Could not open receipt image")
        return bounds
    }

    private fun decodeSampledBitmap(uri: Uri, bounds: BitmapFactory.Options): Bitmap? {
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, MAX_DECODE_DIMENSION)
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    companion object {
        private const val JPEG_QUALITY = 85
        private const val SCALE_FACTOR = 0.75f
        private const val MAX_SCALE_ATTEMPTS = 6
        private const val MAX_DECODE_DIMENSION = 2048

        internal fun calculateInSampleSize(bounds: BitmapFactory.Options, maxDimension: Int): Int {
            require(maxDimension > 0) { "maxDimension must be positive" }
            val height = bounds.outHeight
            val width = bounds.outWidth
            var inSampleSize = 1
            while (height / inSampleSize > maxDimension || width / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }
            return inSampleSize
        }
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return output.toByteArray()
    }
}
