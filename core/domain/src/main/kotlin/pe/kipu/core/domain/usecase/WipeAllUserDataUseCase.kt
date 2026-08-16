package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import pe.kipu.core.domain.repository.UserDataExportFileRepository
import pe.kipu.core.domain.repository.UserDataWipeRepository

class WipeAllUserDataUseCase @Inject constructor(
    private val userDataWipeRepository: UserDataWipeRepository,
    private val exportFileRepository: UserDataExportFileRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val cacheResult = executeStep(exportFileRepository::clearLocalFileCaches)
        val wipeResult = executeStep(userDataWipeRepository::wipeAllUserData)

        if (cacheResult.isSuccess) return wipeResult

        val cacheFailure = requireNotNull(cacheResult.exceptionOrNull())
        wipeResult.exceptionOrNull()
            ?.takeIf { it !== cacheFailure }
            ?.let(cacheFailure::addSuppressed)
        return Result.failure(cacheFailure)
    }

    private suspend fun executeStep(block: suspend () -> Result<Unit>): Result<Unit> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
}
