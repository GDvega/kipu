package pe.kipu.core.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
            val uri = Uri.parse(contentUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val originalBytes = stream.readBytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    error("Invalid receipt image")
                }

                var bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
                    ?: error("Invalid receipt image")

                var jpegBytes = bitmap.toJpegBytes()
                var scaleAttempts = 0
                while (jpegBytes.size > OcrImage.MAX_BYTES && scaleAttempts < MAX_SCALE_ATTEMPTS) {
                    val scaledWidth = (bitmap.width * SCALE_FACTOR).toInt().coerceAtLeast(1)
                    val scaledHeight = (bitmap.height * SCALE_FACTOR).toInt().coerceAtLeast(1)
                    bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
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
            } ?: error("Could not open receipt image")
        }
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return output.toByteArray()
    }

    private companion object {
        const val JPEG_QUALITY = 85
        const val SCALE_FACTOR = 0.75f
        const val MAX_SCALE_ATTEMPTS = 6
    }
}
