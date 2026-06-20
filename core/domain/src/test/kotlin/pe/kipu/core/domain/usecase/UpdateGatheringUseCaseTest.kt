package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository

class UpdateGatheringUseCaseTest {

    private val repository = FakeGatheringRepository(
        Gathering(
            id = "gathering-1",
            name = "Asado",
            participantCount = 2,
            participantNames = listOf("Ana", "Luis"),
        ),
    )
    private val useCase = UpdateGatheringUseCase(repository)

    @Test
    fun updatesExistingGathering() = runTest {
        val result = useCase(
            id = "gathering-1",
            name = " Cena ",
            participantsInput = "Ana\nPedro",
        )

        assertTrue(result is DomainResult.Ok)
        assertEquals("Cena", repository.saved.last().name)
        assertEquals(2, repository.saved.last().participantCount)
    }

    @Test
    fun rejectsUnknownGathering() = runTest {
        val result = useCase(
            id = "missing",
            name = "Test",
            participantsInput = "Ana",
        )

        assertTrue(result is DomainResult.Err)
    }

    private class FakeGatheringRepository(
        initial: Gathering,
    ) : GatheringRepository {
        val saved = mutableListOf<Gathering>()
        private val gatherings = MutableStateFlow(listOf(initial))

        override fun observeGatherings() = gatherings

        override suspend fun getById(id: String): Gathering? =
            gatherings.value.firstOrNull { it.id == id }

        override suspend fun save(gathering: Gathering): Result<Unit> {
            saved += gathering
            gatherings.value = listOf(gathering)
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
