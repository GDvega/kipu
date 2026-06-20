package pe.kipu.core.domain.duplicate

import org.junit.Assert.assertEquals
import org.junit.Test

class MovementDuplicatePairKeysTest {

    @Test
    fun `canonical key is order independent`() {
        assertEquals(
            "movement-a:movement-b",
            canonicalMovementDuplicatePairKey("movement-b", "movement-a"),
        )
    }
}
