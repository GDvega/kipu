package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.UserPreferencesRepository

class CompleteOnboardingUseCaseTest {

    @Test
    fun `marks onboarding complete without pending wizard by default`() = runTest {
        val repository = RecordingUserPreferencesRepository()
        val useCase = CompleteOnboardingUseCase(repository)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(repository.lastSaved?.onboardingCompleted == true)
        assertFalse(repository.lastSaved?.pendingPlanWizard == true)
    }

    @Test
    fun `persists pending plan wizard flag when requested`() = runTest {
        val repository = RecordingUserPreferencesRepository()
        val useCase = CompleteOnboardingUseCase(repository)

        useCase(pendingPlanWizard = true)

        assertTrue(repository.lastSaved?.pendingPlanWizard == true)
    }

    private class RecordingUserPreferencesRepository : UserPreferencesRepository {
        var lastSaved: UserPreferences? = null

        override fun observePreferences(): Flow<UserPreferences> = flowOf(UserPreferences())

        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit> {
            lastSaved = transform(UserPreferences())
            return Result.success(Unit)
        }

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }
}
