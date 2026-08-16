package pe.kipu.core.data.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.data.local.dao.DismissedDuplicatePairDao
import pe.kipu.core.data.local.entity.DismissedDuplicatePairEntity
import pe.kipu.core.domain.time.TimeProvider

class RoomDuplicateDismissalRepositoryTest {

    private val fixedInstant = Instant.parse("2026-08-15T12:00:00Z")
    private val timeProvider = object : TimeProvider {
        override fun now(): Instant = fixedInstant
    }
    private val dao = FakeDismissedDuplicatePairDao()
    private val repository = RoomDuplicateDismissalRepository(dao, timeProvider)

    @Test
    fun dismiss_insertsEntityWithTimeProviderTimestamp() = runTest {
        val result = repository.dismiss("movement-1_movement-2")

        assertTrue(result.isSuccess)
        assertEquals(1, dao.insertedEntities.size)
        val entity = dao.insertedEntities.first()
        assertEquals("movement-1_movement-2", entity.pairKey)
        assertEquals(fixedInstant.toEpochMilli(), entity.dismissedAtMillis)
    }

    @Test
    fun observeDismissedPairKeys_emitsSetOfKeysFromDao() = runTest {
        dao.pairKeysFlow.value = listOf("pair_1", "pair_2", "pair_1")

        val keys = repository.observeDismissedPairKeys().first()

        assertEquals(setOf("pair_1", "pair_2"), keys)
    }

    private class FakeDismissedDuplicatePairDao : DismissedDuplicatePairDao {
        val insertedEntities = mutableListOf<DismissedDuplicatePairEntity>()
        val pairKeysFlow = MutableStateFlow<List<String>>(emptyList())

        override fun observePairKeys(): Flow<List<String>> = pairKeysFlow

        override suspend fun insert(entity: DismissedDuplicatePairEntity) {
            insertedEntities.add(entity)
            pairKeysFlow.value = (pairKeysFlow.value + entity.pairKey).distinct()
        }

        override suspend fun deleteAll() {
            insertedEntities.clear()
            pairKeysFlow.value = emptyList()
        }
    }
}
