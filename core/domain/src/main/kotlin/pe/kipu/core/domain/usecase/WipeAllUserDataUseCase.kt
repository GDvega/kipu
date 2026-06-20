package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.repository.UserDataExportFileRepository
import pe.kipu.core.domain.repository.UserDataWipeRepository

class WipeAllUserDataUseCase @Inject constructor(
    private val userDataWipeRepository: UserDataWipeRepository,
    private val exportFileRepository: UserDataExportFileRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        exportFileRepository.clearLocalFileCaches()
        return userDataWipeRepository.wipeAllUserData()
    }
}
