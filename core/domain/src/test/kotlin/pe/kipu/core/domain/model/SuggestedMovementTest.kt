package pe.kipu.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class SuggestedMovementTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun validate_acceptsReceiptSuggestionWithAmount() {
        val suggestion = SuggestedMovement(
            draftId = "draft-1",
            source = MovementSource.RECEIPT,
            confidence = SuggestionConfidence.HIGH,
            amount = Money.of(BigDecimal("12.50")).getOrError(),
        )
        assertTrue(suggestion.validate() is DomainResult.Ok)
    }

    @Test
    fun validate_rejectsManualSource() {
        val suggestion = SuggestedMovement(
            draftId = "draft-2",
            source = MovementSource.MANUAL,
            amount = Money.of(BigDecimal("1.00")).getOrError(),
        )
        assertTrue(suggestion.validate() is DomainResult.Err)
    }

    @Test
    fun validate_rejectsEmptySuggestion() {
        val suggestion = SuggestedMovement(
            draftId = "draft-3",
            source = MovementSource.NOTIFICATION,
        )
        assertTrue(suggestion.validate() is DomainResult.Err)
    }

    @Test
    fun validate_requiresCategoryReasonWhenCategorySuggested() {
        val suggestion = SuggestedMovement(
            draftId = "draft-4",
            source = MovementSource.RECEIPT,
            amount = Money.of(BigDecimal("5.00")).getOrError(),
            categoryId = "category-food",
        )
        assertTrue(suggestion.validate() is DomainResult.Err)
    }

    @Test
    fun validate_acceptsCategoryWithReason() {
        val suggestion = SuggestedMovement(
            draftId = "draft-5",
            source = MovementSource.RECEIPT,
            confidence = SuggestionConfidence.LOW,
            amount = Money.of(BigDecimal("8.00")).getOrError(),
            categoryId = "category-food",
            categorySuggestionReason = "plin_history_match",
        )
        assertTrue(suggestion.validate() is DomainResult.Ok)
    }

    @Test
    fun toPendingMovement_mapsOperationReferenceToOperationNumber() {
        val suggestion = SuggestedMovement(
            draftId = "draft-6",
            source = MovementSource.RECEIPT,
            confidence = SuggestionConfidence.HIGH,
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("20.00")).getOrError(),
            operationReference = "OP-123456",
        )
        val result = suggestion.toPendingMovement(
            id = "movement-1",
            categoryId = "category-1",
            recordedAt = now,
            createdAt = now,
        )
        assertTrue(result is DomainResult.Ok)
        assertEquals("OP-123456", (result as DomainResult.Ok).value.operationNumber)
    }
}
