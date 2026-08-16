package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.CycleAvailableBudget
import pe.kipu.core.domain.model.HomeInsights
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.widget.DailyAvailableWidgetGateway

class UpdateDailyAvailableWidgetUseCaseTest {

    private val repository = RecordingUserPreferencesRepository()
    private val gateway = RecordingWidgetGateway()
    private val timeProvider = TimeProvider { Instant.parse("2026-08-13T15:30:00Z") }
    private val useCase = UpdateDailyAvailableWidgetUseCase(repository, gateway, timeProvider)

    @Test
    fun persistsFormattedDailyAvailableAndRefreshesWidget() = runTest {
        val amount = Money.of(BigDecimal("42.50")).getOrError()
        val insights = HomeInsights(
            cycleAvailable = CycleAvailableBudget(
                cycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
                periodLabel = "Disponible",
                cycleRemaining = amount,
                cycleDeficit = null,
                cycleAvailable = amount,
                isOverBudget = false,
                daysRemainingInCycle = 3,
            ),
            antSpendingAlerts = emptyList(),
            movementCount = 2,
            envelopeCount = 2,
            userPreferences = UserPreferences(),
        )

        val result = useCase(insights)

        assertTrue(result.isSuccess)
        assertEquals("S/ 42.50", repository.lastSaved?.widgetDailyAvailableText)
        assertEquals(
            Instant.parse("2026-08-13T15:30:00Z").toEpochMilli(),
            repository.lastSaved?.widgetDailyAvailableUpdatedAtMillis,
        )
        assertEquals(1, gateway.refreshCount)
    }

    @Test
    fun skipsPersistenceAndRefreshWhenWidgetSnapshotMatches() = runTest {
        val amount = Money.of(BigDecimal("42.50")).getOrError()
        val currentPreferences = UserPreferences(
            widgetDailyAvailableText = "S/ 42.50",
            widgetIsOverBudget = false,
            widgetDailyAvailableUpdatedAtMillis = Instant.parse("2026-08-12T15:30:00Z").toEpochMilli(),
        )
        repository.currentPreferences = currentPreferences
        val insights = HomeInsights(
            cycleAvailable = CycleAvailableBudget(
                cycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
                periodLabel = "Disponible",
                cycleRemaining = amount,
                cycleDeficit = null,
                cycleAvailable = amount,
                isOverBudget = false,
                daysRemainingInCycle = 3,
            ),
            antSpendingAlerts = emptyList(),
            movementCount = 2,
            envelopeCount = 2,
            userPreferences = currentPreferences,
        )

        val result = useCase(insights)

        assertTrue(result.isSuccess)
        assertEquals(0, repository.updateCount)
        assertEquals(0, gateway.refreshCount)
    }

    @Test
    fun usesSetupMessageWhenNoEnvelopes() = runTest {
        val insights = HomeInsights(
            cycleAvailable = CycleAvailableBudget(
                cycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
                periodLabel = "Disponible",
                cycleRemaining = Money.ZERO,
                cycleDeficit = null,
                cycleAvailable = null,
                isOverBudget = false,
                daysRemainingInCycle = 0,
            ),
            antSpendingAlerts = emptyList(),
            movementCount = 0,
            envelopeCount = 0,
            userPreferences = UserPreferences(),
        )

        useCase(insights)

        assertEquals("Configura sobres", repository.lastSaved?.widgetDailyAvailableText)
    }

    private class RecordingUserPreferencesRepository : UserPreferencesRepository {
        var currentPreferences: UserPreferences = UserPreferences()
        var lastSaved: UserPreferences? = null
        var updateCount: Int = 0

        override fun observePreferences(): Flow<UserPreferences> = flowOf(currentPreferences)

        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit> {
            updateCount++
            currentPreferences = transform(currentPreferences)
            lastSaved = currentPreferences
            return Result.success(Unit)
        }

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }

    private class RecordingWidgetGateway : DailyAvailableWidgetGateway {
        var refreshCount: Int = 0

        override suspend fun requestRefresh() {
            refreshCount++
        }
    }
}
