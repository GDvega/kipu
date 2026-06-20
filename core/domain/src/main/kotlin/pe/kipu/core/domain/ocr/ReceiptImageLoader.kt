package pe.kipu.core.domain.ocr

import pe.kipu.core.domain.model.OcrImage

/**
 * Loads a shared receipt image into memory for local OCR.
 * Implementations must not persist images or log URIs.
 */
interface ReceiptImageLoader {
    suspend fun load(contentUri: String): Result<OcrImage>
}
