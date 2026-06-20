package pe.kipu.core.data.export

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.kipu.core.domain.repository.StoredExportFile
import pe.kipu.core.domain.repository.UserDataExportFileRepository

@Singleton
class AndroidUserDataExportFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserDataExportFileRepository {

    override suspend fun writeExport(
        content: String,
        fileName: String,
        mimeType: String,
    ): Result<StoredExportFile> = runCatching {
        withContext(Dispatchers.IO) {
            val exportDir = exportDirectory()
            exportDir.mkdirs()
            val targetFile = File(exportDir, sanitizeFileName(fileName))
            targetFile.writeText(content, Charsets.UTF_8)
            StoredExportFile(
                absolutePath = targetFile.absolutePath,
                fileName = targetFile.name,
                mimeType = mimeType,
            )
        }
    }

    override suspend fun clearLocalFileCaches(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            clearDirectory(exportDirectory())
            clearDirectory(receiptDirectory())
        }
    }

    private fun exportDirectory(): File = File(context.cacheDir, EXPORT_DIR_NAME)

    private fun receiptDirectory(): File = File(context.cacheDir, RECEIPTS_DIR_NAME)

    private fun clearDirectory(directory: File) {
        if (!directory.exists()) return
        directory.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        val sanitized = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return sanitized.ifBlank { "kipu_export.dat" }
    }

    companion object {
        const val EXPORT_DIR_NAME = "exports"
        const val RECEIPTS_DIR_NAME = "receipts"
    }
}
