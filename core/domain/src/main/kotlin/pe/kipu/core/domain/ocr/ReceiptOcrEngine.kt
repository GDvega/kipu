package pe.kipu.core.domain.ocr

import pe.kipu.core.domain.model.OcrImage

/**
 * Local OCR engine contract. Implementations must not log recognized text.
 */
interface ReceiptOcrEngine {
    suspend fun recognize(image: OcrImage): Result<String>
}
