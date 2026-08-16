package pe.kipu.core.domain.export

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ThemeMode
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.model.getOrError

class UserDataCsvSerializerTest {

    private val serializer = UserDataCsvSerializer()
    private val instant = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `serializes movements as csv with header`() {
        val snapshot = sampleSnapshot()

        val lines = serializer.serializeMovements(snapshot).trim().lines()

        assertEquals(
            "id,type,amount,categoryId,channel,source,status,description,counterpartyName,operationNumber,recordedAt,createdAt",
            lines.first(),
        )
        assertEquals(
            "movement-1,EXPENSE,10.00,category-food,YAPE,RECEIPT,CONFIRMED,,Ana,,2026-06-16T12:00:00Z,2026-06-16T12:00:00Z",
            lines[1],
        )
    }

    @Test
    fun `serializes movements with semicolon for Excel Peru`() {
        val snapshot = sampleSnapshot()
        val lines = serializer.serializeMovements(snapshot, delimiter = ';').trim().lines()

        assertEquals(
            "id;type;amount;categoryId;channel;source;status;description;counterpartyName;operationNumber;recordedAt;createdAt",
            lines.first(),
        )
        assertEquals(
            "movement-1;EXPENSE;10.00;category-food;YAPE;RECEIPT;CONFIRMED;;Ana;;2026-06-16T12:00:00Z;2026-06-16T12:00:00Z",
            lines[1],
        )
    }

    private fun sampleSnapshot(): UserDataSnapshot = UserDataSnapshot(
        exportedAt = instant,
        movements = listOf(
            Movement(
                id = "movement-1",
                type = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("10.00")).getOrError(),
                categoryId = "category-food",
                channel = PaymentChannel.YAPE,
                source = MovementSource.RECEIPT,
                status = MovementStatus.CONFIRMED,
                counterpartyName = "Ana",
                recordedAt = instant,
                createdAt = instant,
            ),
        ),
        categories = emptyList(),
        envelopes = emptyList(),
        commitments = emptyList(),
        financialPlans = emptyList(),
        gatherings = emptyList(),
        gatheringExpenses = emptyList(),
        dismissedDuplicatePairKeys = emptySet(),
        preferences = UserPreferences(themeMode = ThemeMode.SYSTEM),
    )
}
