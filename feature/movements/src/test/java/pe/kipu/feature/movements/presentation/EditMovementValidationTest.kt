package pe.kipu.feature.movements.presentation

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.feature.movements.ui.EditMovementFormState

class EditMovementValidationTest {

    private val now = Instant.parse("2026-08-22T12:00:00Z")

    @Test
    fun `fromMovement creates pre-populated valid form`() {
        val movement = Movement(
            id = "mov-1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("42.50")).getOrError(),
            categoryId = "food",
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            description = "Almuerzo",
            counterpartyName = "Restaurante",
            recordedAt = now,
            createdAt = now,
        )

        val form = EditMovementFormState.fromMovement(movement)

        assertEquals("mov-1", form.movementId)
        assertEquals(MovementType.EXPENSE, form.movementType)
        assertEquals(PaymentChannel.YAPE, form.channel)
        assertEquals("42.5", form.amountText)
        assertEquals("food", form.categoryId)
        assertEquals("Almuerzo", form.description)
        assertEquals("Restaurante", form.counterpartyName)
        assertNull(form.amountErrorMessage)
        assertNull(form.errorMessage)
        assertFalse(form.isSaving)
        assertTrue(form.canSave)
    }

    @Test
    fun `canSave returns false when amount is zero or invalid`() {
        val form = EditMovementFormState(
            movementId = "mov-1",
            movementType = MovementType.EXPENSE,
            channel = PaymentChannel.CASH,
            amountText = "0",
            categoryId = "food",
            description = "",
            counterpartyName = "",
        )

        assertFalse(form.canSave)
    }

    @Test
    fun `canSave returns false when category is blank`() {
        val form = EditMovementFormState(
            movementId = "mov-1",
            movementType = MovementType.EXPENSE,
            channel = PaymentChannel.CASH,
            amountText = "20.00",
            categoryId = "",
            description = "",
            counterpartyName = "",
        )

        assertFalse(form.canSave)
    }

    @Test
    fun `canSave returns false when isSaving is true`() {
        val form = EditMovementFormState(
            movementId = "mov-1",
            movementType = MovementType.EXPENSE,
            channel = PaymentChannel.CASH,
            amountText = "20.00",
            categoryId = "food",
            description = "",
            counterpartyName = "",
            isSaving = true,
        )

        assertFalse(form.canSave)
    }
}
