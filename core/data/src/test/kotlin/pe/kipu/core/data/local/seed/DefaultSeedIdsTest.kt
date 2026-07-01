package pe.kipu.core.data.local.seed

import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSeedIdsTest {

    @Test
    fun newUserHasNoDemoCommitmentSeed() {
        assertTrue(DefaultCommitmentSeed.commitments.isEmpty())
    }

    @Test
    fun financialPlanSeedIsNotPersistedAutomatically() {
        val recordingDb = RecordingSqliteDatabase()

        DefaultFinancialPlanSeed.insertInto(recordingDb)

        assertTrue(recordingDb.executedSql.isEmpty())
    }
}
