package pe.kipu.core.domain.repository

data class StoredExportFile(
    val absolutePath: String,
    val fileName: String,
    val mimeType: String,
)

interface UserDataExportFileRepository {
    suspend fun writeExport(content: String, fileName: String, mimeType: String): Result<StoredExportFile>

    /** Clears export files and ephemeral receipt images from app cache. */
    suspend fun clearLocalFileCaches(): Result<Unit>
}
