package pe.kipu.core.data.test

import java.time.Instant
import pe.kipu.core.data.local.entity.CategoryEntity
import pe.kipu.core.data.local.entity.MovementEntity
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel

internal object MapperTestFixtures {
    val recordedAt: Instant = Instant.parse("2026-06-16T15:30:00Z")
    val createdAt: Instant = Instant.parse("2026-06-16T15:31:00Z")

    fun sampleMovementEntity(
        operationNumber: String? = "OP-123",
        counterpartyName: String? = "María",
        description: String? = "Almuerzo",
    ) = MovementEntity(
        id = "movement-1",
        type = MovementType.EXPENSE.name,
        amountCents = 1_550L,
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE.name,
        source = MovementSource.MANUAL.name,
        status = MovementStatus.CONFIRMED.name,
        description = description,
        counterpartyName = counterpartyName,
        operationNumber = operationNumber,
        recordedAtMillis = recordedAt.toEpochMilli(),
        createdAtMillis = createdAt.toEpochMilli(),
    )

    fun sampleCategoryEntity() = CategoryEntity(
        id = CategoryIds.FOOD,
        name = "Comida",
        iconKey = "food",
    )
}
