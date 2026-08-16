package pe.kipu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.CommitmentsInsights
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.feature.commitments.presentation.CommitmentsUiState
import pe.kipu.feature.envelopes.presentation.EnvelopesUiState
import pe.kipu.feature.movements.ui.ManualMovementFormState

class AsyncActionGuardsTest {

    @Test
    fun deleteGuardsRejectASecondConfirmationAndKeepFailureVisible() {
        val commitment = CommitmentsUiState.Content(
            insights = CommitmentsInsights(emptyList(), null),
            deleteTargetId = "commitment-1",
            deleteTargetTitle = "Meta",
        )
        assertTrue(commitment.canConfirmDelete)
        assertFalse(commitment.copy(isDeleting = true).canConfirmDelete)
        val failedCommitment = commitment.copy(deleteErrorMessage = "No pudimos eliminar")
        assertEquals("No pudimos eliminar", failedCommitment.deleteErrorMessage)
        assertTrue(failedCommitment.deleteTargetId != null)

        val envelopeTarget = envelopeTarget()
        val envelope = EnvelopesUiState.Content(
            budgets = emptyList(),
            categories = emptyList(),
            usedCategoryIds = emptySet(),
            deleteTarget = envelopeTarget,
        )
        assertTrue(envelope.canConfirmDelete)
        assertFalse(envelope.copy(isDeleting = true).canConfirmDelete)
        val failedEnvelope = envelope.copy(deleteErrorMessage = "No pudimos eliminar")
        assertEquals("No pudimos eliminar", failedEnvelope.deleteErrorMessage)
        assertTrue(failedEnvelope.deleteTarget != null)
    }

    @Test
    fun submitGuardsRejectASecondConfirmationWhileSaving() {
        val movement = ManualMovementFormState(
            amountText = "25.50",
            categoryId = "food",
        )
        assertTrue(movement.canSave)
        assertFalse(movement.copy(isSaving = true).canSave)
    }

    private fun envelopeTarget() = EnvelopeBudgetState(
        envelopeId = "envelope-1",
        name = "Comida",
        categoryId = "food",
        weeklyLimit = Money.ZERO,
        spentAmount = Money.ZERO,
        remainingAmount = Money.ZERO,
        percentUsed = 0,
        status = EnvelopeBudgetStatus.OK,
    )
}

