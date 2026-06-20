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

    private class FakeUserDataWipeRepository : UserDataWipeRepository {
        var wipeCalled = false

        override suspend fun wipeAllUserData(): Result<Unit> {
            wipeCalled = true
            return Result.success(Unit)
        }
    }

    private class FakeUserDataExportFileRepository : UserDataExportFileRepository {
        var localCachesCleared = false

        override suspend fun writeExport(
            content: String,
            fileName: String,
            mimeType: String,
        ): Result<pe.kipu.core.domain.repository.StoredExportFile> =
            Result.failure(UnsupportedOperationException())

        override suspend fun clearLocalFileCaches(): Result<Unit> {
            localCachesCleared = true
            return Result.success(Unit)
        }
    }
}
