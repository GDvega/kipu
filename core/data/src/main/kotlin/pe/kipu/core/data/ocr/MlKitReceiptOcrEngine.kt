package pe.kipu.core.data.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.ocr.ReceiptOcrEngine

@Singleton
class MlKitReceiptOcrEngine @Inject constructor() : ReceiptOcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: OcrImage): Result<String> = runCatching {
        val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
            ?: throw IllegalArgumentException("Invalid image bytes")
        val inputImage = InputImage.fromBitmap(bitmap, image.rotationDegrees)
        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val sanitized = visionText.text
                        .trim()
                        .replace(Regex("\\s+"), " ")
                        .take(MAX_OUTPUT_CHARS)
                    continuation.resume(sanitized)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }

    private companion object {
        const val MAX_OUTPUT_CHARS: Int = 20_000
    }
}
