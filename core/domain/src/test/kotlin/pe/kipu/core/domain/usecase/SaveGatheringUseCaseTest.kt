package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import java.time.Instant

class SaveGatheringUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val repository = FakeGatheringRepository()
    private val useCase = SaveGatheringUseCase(
        gatheringRepository = repository,
        timeProvider = FixedTimeProvider(now),
    )

    @Test
    fun savesGatheringWithParsedParticipants() = runTest {
        val result = useCase(
            name = " Cena grupal ",
            participantsInput = "Ana\nLuis",
        )

        assertTrue(result is DomainResult.Ok)
        assertEquals(1, repository.saved.size)
        assertEquals("gathering-${now.toEpochMilli()}", repository.saved.first().id)
        assertEquals(2, repository.saved.first().participantCount)
    }

    @Test
    fun rejectsBlankName() = runTest {
        val result = useCase(name = "  ", participantsInput = "Ana")

        assertTrue(result is DomainResult.Err)
        assertEquals(0, repository.saved.size)
    }

    private class FakeGatheringRepository : GatheringRepository {
        val saved = mutableListOf<Gathering>()
        private val gatherings = MutableStateFlow<List<Gathering>>(emptyList())

        override fun observeGatherings() = gatherings

        override suspend fun getById(id: String): Gathering? = null

        override suspend fun save(gathering: Gathering): Result<Unit> {
            saved += gathering
            gatherings.value = listOf(gathering)
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
