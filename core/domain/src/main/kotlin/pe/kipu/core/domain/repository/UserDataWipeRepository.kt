package pe.kipu.core.domain.repository

interface UserDataWipeRepository {
    suspend fun wipeAllUserData(): Result<Unit>
}
