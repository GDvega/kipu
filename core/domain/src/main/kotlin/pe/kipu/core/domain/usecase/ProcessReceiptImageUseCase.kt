package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.ocr.ReceiptOcrEngine

/**
 * Runs local OCR then delegates parsing to domain [ParseReceiptTextUseCase].
 * Does not persist images or log recognized text.
 */
class ProcessReceiptImageUseCase @Inject constructor(
    private val receiptOcrEngine: ReceiptOcrEngine,
    private val parseReceiptText: ParseReceiptTextUseCase,
) {

    suspend operator fun invoke(image: OcrImage): ReceiptParseResult {
        val text = receiptOcrEngine.recognize(image).getOrElse {
            return ReceiptParseResult.Failure(
                DomainError.InvalidField("Could not recognize receipt text"),
            )
        }
        if (text.isBlank()) {
            return ReceiptParseResult.Failure(DomainError.InvalidField("Empty OCR result"))
        }
        return parseReceiptText(text)
    }
}
