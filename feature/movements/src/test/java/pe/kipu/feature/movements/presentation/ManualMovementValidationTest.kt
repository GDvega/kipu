package pe.kipu.feature.movements.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.feature.movements.ui.ManualMovementFormState

class ManualMovementValidationTest {

    @Test
    fun initialFormHidesAmountErrorAndCannotSaveUntilAmountIsValid() {
        val initial = ManualMovementFormState(categoryId = "food")

        assertNull(initial.amountErrorMessage)
        assertFalse(initial.canSave)
        assertTrue(initial.copy(amountText = "25.50").canSave)
    }

    @Test
    fun emptyAmountShowsRequiredMessage() {
        assertEquals(
            "Ingresa un monto",
            ManualMovementAmountValidator.errorMessage(""),
        )
        assertFalse(ManualMovementAmountValidator.isValid(""))
    }

    @Test
    fun zeroAmountShowsGreaterThanZeroMessage() {
        assertEquals(
            "El monto debe ser mayor a cero",
            ManualMovementAmountValidator.errorMessage("0"),
        )
        assertFalse(ManualMovementAmountValidator.isValid("0"))
    }

    @Test
    fun invalidAmountShowsFormatMessage() {
        assertEquals(
            "Revisa el formato del monto",
            ManualMovementAmountValidator.errorMessage("25.."),
        )
        assertFalse(ManualMovementAmountValidator.isValid("25.."))
    }

    @Test
    fun applyPresetWhenEmptySetsExactAmount() {
        assertEquals(
            "10",
            ManualMovementAmountValidator.applyPreset("", java.math.BigDecimal("10")),
        )
    }

    @Test
    fun applyPresetWithExistingAmountAddsCorrectly() {
        assertEquals(
            "35",
            ManualMovementAmountValidator.applyPreset("25", java.math.BigDecimal("10")),
        )
        assertEquals(
            "17.50",
            ManualMovementAmountValidator.applyPreset("12.50", java.math.BigDecimal("5")),
        )
    }
}

