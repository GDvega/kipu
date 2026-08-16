package pe.kipu.core.data.local.seed

import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSeedIdsTest {

    @Test
    fun newUserHasNoDemoCommitmentSeed() {
        assertTrue(DefaultCommitmentSeed.commitments.isEmpty())
    }
}
