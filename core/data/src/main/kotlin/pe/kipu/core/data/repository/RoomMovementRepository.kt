package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import pe.kipu.core.data.local.KipuDatabase
import pe.kipu.core.data.local.entity.ReserveEventEntity
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.MovementRepository

@Singleton
class RoomMovementRepository @Inject constructor(
    private val database: KipuDatabase,
) : MovementRepository {
    private val movementDao get() = database.movementDao()

    override fun observeMovements(): Flow<List<Movement>> =
        movementDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: EntityId): Movement? =
        movementDao.getById(id)?.toDomain()

    override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
        movementDao.findByCounterpartyName(counterpartyName).map { it.toDomain() }

    override suspend fun save(movement: Movement): Result<Unit> {
        when (val validation = movement.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { movementDao.upsert(movement.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { movementDao.deleteById(id) }

    override suspend fun mergeDuplicates(kept: Movement, removed: Movement): Result<Unit> = try {
        database.withTransaction {
            require(kept.id != removed.id && getById(kept.id) == kept && getById(removed.id) == removed) {
                "Los movimientos cambiaron; revisa el duplicado nuevamente"
            }
            require(kept.type == removed.type && kept.amount == removed.amount &&
                kept.status == MovementStatus.CONFIRMED && removed.status == MovementStatus.CONFIRMED) {
                "Los movimientos no representan la misma operación confirmada"
            }
            val merged = kept.copy(
                envelopeId = mergeLink(kept.envelopeId, removed.envelopeId),
                commitmentId = mergeLink(kept.commitmentId, removed.commitmentId),
                operationNumber = mergeLink(kept.operationNumber, removed.operationNumber),
            )
            val ids = setOf(kept.id, removed.id)
            val receiptDao = database.monthlyServiceReceiptDao()
            val linkedReceipts = receiptDao.getAll().filter { it.isPaid && it.paidMovementId in ids }
            require(linkedReceipts.size <= 1) { "Los movimientos pagan recibos diferentes; revisa sus vínculos" }
            val month = YearMonth.from(kept.recordedAt.atZone(ZoneId.of("America/Lima"))).toString()
            require(linkedReceipts.all { it.monthKey == month }) { "El pago pertenece a otro mes" }
            val gatheringDao = database.gatheringExpenseDao()
            require(!(gatheringDao.isMovementLinked(kept.id) && gatheringDao.isMovementLinked(removed.id))) {
                "Ambos movimientos tienen gastos compartidos; revisa sus vínculos"
            }
            val reserveDao = database.reserveEventDao()
            val events = reserveDao.observeAll().first()
            val reversedIds = events.filter { it.type == "REVERSAL" }.mapNotNull { it.reversesEventId }.toSet()
            val active = events.filter { it.sourceMovementId in ids && it.id !in reversedIds && it.type != "REVERSAL" }
            require(active.all { it.type == "USE" }) { "Hay devoluciones de reserva que requieren revisión" }
            require(active.size <= 2 && active.map { it.sourceMovementId }.distinct().size == active.size) {
                "Hay usos de reserva inconsistentes"
            }
            val reserveCents = active.maxOfOrNull { it.amountCents } ?: 0L
            require(reserveCents <= kept.toEntity().amountCents) { "La reserva supera el importe de la operación" }
            val now = Instant.now()
            // One confirmed operation retains one reserve use, never the sum of duplicate uses.
            active.forEach { event ->
                reserveDao.insertValidated(
                    ReserveEventEntity(
                        id = "reserve-reversal-${UUID.randomUUID()}", type = "REVERSAL",
                        amountCents = event.amountCents, reversesEventId = event.id,
                        occurredAtMillis = now.toEpochMilli(), createdAtMillis = now.toEpochMilli(),
                    ),
                )
            }
            if (reserveCents > 0L) {
                reserveDao.insertValidated(
                    ReserveEventEntity(
                        id = "reserve-use-${UUID.randomUUID()}", type = "USE", amountCents = reserveCents,
                        sourceMovementId = kept.id, occurredAtMillis = now.toEpochMilli(),
                        createdAtMillis = now.toEpochMilli(),
                    ),
                )
            }
            movementDao.upsert(merged.toEntity())
            linkedReceipts.forEach { receipt ->
                receiptDao.upsert(receipt.copy(paidMovementId = kept.id, paidAtEpochMs = kept.recordedAt.toEpochMilli()))
            }
            gatheringDao.transferMovementLink(removed.id, kept.id)
            movementDao.deleteById(removed.id)
            database.movementAuditDao().insert(
                MovementAuditEntry(
                    id = UUID.randomUUID().toString(), movementId = removed.id,
                    action = MovementAuditAction.DELETED, movementType = removed.type, amount = removed.amount,
                    categoryId = removed.categoryId, channel = removed.channel,
                    details = "Duplicado fusionado; vínculos conservados", timestamp = now,
                ).toEntity(),
            )
        }
        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun mergeLink(first: String?, second: String?): String? {
        require(first == null || second == null || first == second) { "Los vínculos son distintos; revísalos antes de fusionar" }
        return first ?: second
    }
}
