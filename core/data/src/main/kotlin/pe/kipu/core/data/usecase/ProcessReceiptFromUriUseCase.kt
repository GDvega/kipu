package pe.kipu.core.data.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.ocr.ReceiptImageLoader

/**
 * Loads a shared receipt image and runs local OCR + domain parsing.
 * Does not persist images or log URIs.
 */
class ProcessReceiptFromUriUseCase @Inject constructor(
    private val receiptImageLoader: ReceiptImageLoader,
    private val processReceiptImage: ProcessReceiptImageUseCase,
) {

    suspend operator fun invoke(contentUri: String): ReceiptParseResult {
        val image = receiptImageLoader.load(contentUri).getOrElse {
            return ReceiptParseResult.Failure(
                DomainError.InvalidField("Could not load receipt image"),
            )
        }
        return processReceiptImage(image)
    }
}
