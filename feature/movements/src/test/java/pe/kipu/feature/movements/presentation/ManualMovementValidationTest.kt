package pe.kipu.feature.movements.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualMovementValidationTest {

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
    fun validAmountHasNoError() {
        assertNull(ManualMovementAmountValidator.errorMessage("25.50"))
        assertTrue(ManualMovementAmountValidator.isValid("25.50"))
    }
}
