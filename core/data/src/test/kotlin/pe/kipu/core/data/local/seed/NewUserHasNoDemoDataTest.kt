package pe.kipu.core.data.local.seed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewUserHasNoDemoDataTest {

    @Test
    fun defaultCommitmentSeedDoesNotContainDemoGoalsOrDebts() {
        val titles = DefaultCommitmentSeed.commitments.map { it.title }

        assertTrue(DefaultCommitmentSeed.commitments.isEmpty())
        assertFalse("Fondo emergencia" in titles)
        assertFalse("Deuda con Juan" in titles)
    }

    @Test
    fun baselineSeederDoesNotInsertDemoCommitmentsEnvelopesOrFinancialPlan() {
        val recordingDb = RecordingSqliteDatabase()

        pe.kipu.core.data.local.KipuDatabaseSeeder.seedBaseline(recordingDb)

        val sql = recordingDb.executedSql.joinToString(separator = "\n")
        assertFalse(sql.contains("INSERT OR IGNORE INTO commitments", ignoreCase = true))
        assertFalse(sql.contains("INSERT OR IGNORE INTO envelopes", ignoreCase = true))
        assertFalse(sql.contains("INSERT OR IGNORE INTO financial_plans", ignoreCase = true))
    }
}
