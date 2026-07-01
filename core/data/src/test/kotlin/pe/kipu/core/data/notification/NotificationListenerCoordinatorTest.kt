package pe.kipu.core.data.notification

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.notification.MonitoredPaymentApps
import pe.kipu.core.domain.parser.NotificationParserRouter
import pe.kipu.core.domain.parser.PlinIncomeNotificationParser
import pe.kipu.core.domain.parser.YapeIncomeNotificationParser
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.usecase.DetectDuplicateMovementUseCase
import pe.kipu.core.domain.usecase.EvaluateAutoApprovalUseCase
import pe.kipu.core.domain.usecase.ParseNotificationTextUseCase
import pe.kipu.core.domain.usecase.RegisterNotificationIncomeUseCase
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher

class NotificationListenerCoordinatorTest {

    private val parseNotificationText = ParseNotificationTextUseCase(
        notificationParserRouter = NotificationParserRouter(
            yapeIncomeNotificationParser = YapeIncomeNotificationParser(),
            plinIncomeNotificationParser = PlinIncomeNotificationParser(),
        ),
    )

    private val yapeIncomeLines = NotificationTestFixtures
        .load("notifications/yape_income_standard.txt")
        .trim()
        .lines()

    @Test
    fun `skips processing when notifications preference is disabled`() = runTest {
        val repository = RecordingMovementRepository()
        val coordinator = buildCoordinator(
            repository = repository,
            notificationsEnabled = false,
            scope = this,
        )

        coordinator.processNotification(
            packageName = MonitoredPaymentApps.YAPE_PACKAGE,
            title = yapeIncomeLines.first(),
            text = yapeIncomeLines.getOrNull(1),
        )

        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `onNotificationPosted ignores unmonitored packages`() = runTest {
        val repository = RecordingMovementRepository()
        val coordinator = buildCoordinator(
            repository = repository,
            notificationsEnabled = true,
            scope = this,
        )

        coordinator.onNotificationPosted(
            packageName = "com.unknown.wallet",
            title = "Yape",
            text = yapeIncomeLines.getOrNull(1),
        )
        advanceUntilIdle()

        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `onNotificationPosted respects notifications preference`() = runTest {
        val repository = RecordingMovementRepository()
        val coordinator = buildCoordinator(
            repository = repository,
            notificationsEnabled = false,
            scope = this,
        )

        coordinator.onNotificationPosted(
            packageName = MonitoredPaymentApps.YAPE_PACKAGE,
            title = yapeIncomeLines.first(),
            text = yapeIncomeLines.getOrNull(1),
        )
        advanceUntilIdle()

        assertEquals(0, repository.saveCount)
    }

    private fun buildCoordinator(
        repository: RecordingMovementRepository,
        notificationsEnabled: Boolean,
        scope: kotlinx.coroutines.CoroutineScope,
    ): NotificationListenerCoordinator {
        val userPrefs = FixedUserPreferencesRepository(notificationsEnabled)
        return NotificationListenerCoordinator(
            userPreferencesRepository = userPrefs,
            parseNotificationText = parseNotificationText,
            registerNotificationIncome = RegisterNotificationIncomeUseCase(
                movementRepository = repository,
                timeProvider = FixedInstantTimeProvider(Instant.parse("2026-06-16T15:00:00Z")),
                userPreferencesRepository = userPrefs,
                detectDuplicateMovement = DetectDuplicateMovementUseCase(MovementDuplicateMatcher()),
                evaluateAutoApproval = EvaluateAutoApprovalUseCase(),
            ),
            applicationScope = scope,
        )
    }

    private class FixedUserPreferencesRepository(
        private val notificationsEnabled: Boolean,
    ) : UserPreferencesRepository {
        override fun observePreferences(): Flow<UserPreferences> =
            flowOf(UserPreferences(notificationsEnabled = notificationsEnabled))

        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit> =
            Result.success(Unit)

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }

    private class RecordingMovementRepository : MovementRepository {
        var saveCount: Int = 0

        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())

        override suspend fun getById(id: String): Movement? = null

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saveCount++
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FixedInstantTimeProvider(
        private val instant: Instant,
    ) : pe.kipu.core.domain.time.TimeProvider {
        override fun now(): Instant = instant
    }
}
