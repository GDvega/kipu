package pe.kipu.core.domain.export

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError

class UserDataJsonSerializerTest {

    private val serializer = UserDataJsonSerializer()
    private val instant = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `serializes movement and preferences`() {
        val snapshot = UserDataSnapshot(
            exportedAt = instant,
            movements = listOf(sampleMovement()),
            categories = listOf(Category(id = "category-food", name = "Comida")),
            envelopes = emptyList(),
            commitments = emptyList(),
            financialPlans = emptyList(),
            gatherings = emptyList(),
            dismissedDuplicatePairKeys = setOf("pair-1"),
            preferences = UserPreferences(
                themeMode = ThemeMode.DARK,
                notificationsEnabled = true,
                onboardingCompleted = true,
            ),
        )

        val json = serializer.serialize(snapshot)

        assertTrue(json.contains("\"exportVersion\":2"))
        assertTrue(json.contains("\"counterpartyName\":\"María\""))
        assertTrue(json.contains("\"themeMode\":\"DARK\""))
        assertTrue(json.contains("\"dismissedDuplicatePairKeys\":[\"pair-1\"]"))
    }

    private fun sampleMovement(): Movement = Movement(
        id = "movement-1",
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.50")).getOrError(),
        categoryId = "category-food",
        channel = PaymentChannel.YAPE,
        source = MovementSource.RECEIPT,
        status = MovementStatus.CONFIRMED,
        counterpartyName = "María",
        recordedAt = instant,
        createdAt = instant,
    )
}
