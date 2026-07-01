package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.RegisterNotificationIncomeResult
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher

class RegisterNotificationIncomeUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val repository = RecordingMovementRepository()
    private val userPrefsRepo = RecordingUserPreferencesRepository()
    private val detectDuplicate = DetectDuplicateMovementUseCase(MovementDuplicateMatcher())
    private val evaluateAutoApproval = EvaluateAutoApprovalUseCase()
    private val useCase = RegisterNotificationIncomeUseCase(
        movementRepository = repository,
        timeProvider = FixedTimeProvider(now),
        userPreferencesRepository = userPrefsRepo,
        detectDuplicateMovement = detectDuplicate,
        evaluateAutoApproval = evaluateAutoApproval,
    )

    @Test
    fun `saves pending movement without auto confirming`() = runTest {
        val result = useCase(suggestion())

        assertTrue(result is RegisterNotificationIncomeResult.Saved)
        val saved = (result as RegisterNotificationIncomeResult.Saved).movement
        assertEquals(MovementStatus.PENDING_CONFIRMATION, saved.status)
        assertEquals(MovementSource.NOTIFICATION, saved.source)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `auto confirms when preferences enabled and high confidence`() = runTest {
        userPrefsRepo.preferences = pe.kipu.core.domain.model.UserPreferences(autoApproveHighConfidenceNotifications = true)
        val highConfSuggestion = suggestion().copy(
            operationReference = "123456"
        )
        
        val result = useCase(highConfSuggestion)
        
        assertTrue(result is RegisterNotificationIncomeResult.AutoApproved)
        val saved = (result as RegisterNotificationIncomeResult.AutoApproved).movement
        assertEquals(MovementStatus.CONFIRMED, saved.status)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `does not create second pending with same draft id`() = runTest {
        useCase(suggestion())
        val second = useCase(suggestion())

        assertEquals(RegisterNotificationIncomeResult.AlreadyPending, second)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `creates new pending after prior notification was confirmed`() = runTest {
        val baseId = "movement-notif-yape-5000-maria"
        repository.movements = listOf(confirmedMovement(id = baseId))

        val result = useCase(suggestion())

        assertTrue(result is RegisterNotificationIncomeResult.Saved)
        val saved = (result as RegisterNotificationIncomeResult.Saved).movement
        assertEquals(MovementStatus.PENDING_CONFIRMATION, saved.status)
        assertEquals("$baseId-${now.toEpochMilli()}", saved.id)
        assertEquals(1, repository.saveCount)
        assertEquals(2, repository.movements.size)
    }

    private fun confirmedMovement(id: String): Movement = Movement(
        id = id,
        type = MovementType.INCOME,
        amount = Money.of(BigDecimal("50.00")).getOrError(),
        categoryId = CategoryIds.OTHER,
        channel = PaymentChannel.YAPE,
        source = MovementSource.NOTIFICATION,
        status = MovementStatus.CONFIRMED,
        counterpartyName = "MARIA GARCIA RIOS",
        recordedAt = now,
        createdAt = now,
    )

    private fun suggestion(): SuggestedMovement = SuggestedMovement(
        draftId = "notif-yape-5000-maria",
        source = MovementSource.NOTIFICATION,
        confidence = SuggestionConfidence.HIGH,
        type = MovementType.INCOME,
        amount = Money.of(BigDecimal("50.00")).getOrError(),
        categoryId = CategoryIds.OTHER,
        categorySuggestionReason = "notification_pattern_match",
        channel = PaymentChannel.YAPE,
        counterpartyName = "MARIA GARCIA RIOS",
    )

    private class RecordingMovementRepository : MovementRepository {
        var movements: List<Movement> = emptyList()
        var saveCount: Int = 0

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saveCount++
            movements = movements.filter { it.id != movement.id } + movement
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class RecordingUserPreferencesRepository : UserPreferencesRepository {
        var preferences = UserPreferences()

        override fun observePreferences(): Flow<UserPreferences> = flowOf(preferences)

        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences): Result<Unit> {
            preferences = transform(preferences)
            return Result.success(Unit)
        }

        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }
}
