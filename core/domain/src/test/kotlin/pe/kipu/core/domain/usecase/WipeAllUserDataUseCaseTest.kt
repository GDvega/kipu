package pe.kipu.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.repository.UserDataExportFileRepository
import pe.kipu.core.domain.repository.UserDataWipeRepository

class WipeAllUserDataUseCaseTest {

    @Test
    fun `clears export cache before wiping data`() = runTest {
        val wipeRepository = FakeUserDataWipeRepository()
        val exportRepository = FakeUserDataExportFileRepository()
        val useCase = WipeAllUserDataUseCase(wipeRepository, exportRepository)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(exportRepository.localCachesCleared)
        assertTrue(wipeRepository.wipeCalled)
    }

    @Test
    fun `reports cache cleanup failure after still attempting data wipe`() = runTest {
        val cacheFailure = IllegalStateException("cache cleanup failed")
        val wipeRepository = FakeUserDataWipeRepository()
        val exportRepository = FakeUserDataExportFileRepository(
            clearResult = Result.failure(cacheFailure),
        )
        val useCase = WipeAllUserDataUseCase(wipeRepository, exportRepository)

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() === cacheFailure)
        assertTrue(exportRepository.localCachesCleared)
        assertTrue(wipeRepository.wipeCalled)
    }

    @Test
    fun `converts unexpected cache exception to failure after still attempting data wipe`() = runTest {
        val cacheFailure = IllegalStateException("cache repository crashed")
        val wipeRepository = FakeUserDataWipeRepository()
        val exportRepository = FakeUserDataExportFileRepository(
            clearException = cacheFailure,
        )
        val useCase = WipeAllUserDataUseCase(wipeRepository, exportRepository)

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() === cacheFailure)
        assertTrue(wipeRepository.wipeCalled)
    }

    @Test
    fun `keeps cache failure and attaches wipe failure when both steps fail`() = runTest {
        val cacheFailure = IllegalStateException("cache cleanup failed")
        val wipeFailure = IllegalStateException("database wipe failed")
        val wipeRepository = FakeUserDataWipeRepository(
            wipeResult = Result.failure(wipeFailure),
        )
        val exportRepository = FakeUserDataExportFileRepository(
            clearResult = Result.failure(cacheFailure),
        )
        val useCase = WipeAllUserDataUseCase(wipeRepository, exportRepository)

        val result = useCase()

        assertTrue(result.exceptionOrNull() === cacheFailure)
        assertTrue(result.exceptionOrNull()?.suppressed?.singleOrNull() === wipeFailure)
    }

    private class FakeUserDataWipeRepository(
        private val wipeResult: Result<Unit> = Result.success(Unit),
    ) : UserDataWipeRepository {
        var wipeCalled = false

        override suspend fun wipeAllUserData(): Result<Unit> {
            wipeCalled = true
            return wipeResult
        }
    }

    private class FakeUserDataExportFileRepository(
        private val clearResult: Result<Unit> = Result.success(Unit),
        private val clearException: Throwable? = null,
    ) : UserDataExportFileRepository {
        var localCachesCleared = false

        override suspend fun writeExport(
            content: String,
            fileName: String,
            mimeType: String,
        ): Result<pe.kipu.core.domain.repository.StoredExportFile> =
            Result.failure(UnsupportedOperationException())

        override suspend fun clearLocalFileCaches(): Result<Unit> {
            localCachesCleared = true
            clearException?.let { throw it }
            return clearResult
        }
    }
}
