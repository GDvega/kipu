package pe.kipu.core.data.local.seed

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.plan.CommitmentIds
import pe.kipu.core.domain.plan.FinancialPlanIds

class DefaultSeedIdsTest {

    @Test
    fun financialPlanSeedUsesDomainPrimaryId() {
        assertEquals(FinancialPlanIds.PRIMARY, DefaultFinancialPlanSeed.plan.id)
    }

    @Test
    fun emergencyFundSeedUsesDomainCommitmentId() {
        assertEquals(CommitmentIds.EMERGENCY_FUND, DefaultCommitmentSeed.commitments.first().id)
    }
}
