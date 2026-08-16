package pe.kipu.core.data.export

/**
 * Sanitizes export file names to prevent path traversal, file system errors,
 * and malicious character injections.
 */
object ExportFileNameSanitizer {

    private const val DEFAULT_FILE_NAME = "kipu_export.dat"
    private const val DEFAULT_BASE_NAME = "kipu_export"
    private const val DEFAULT_EXTENSION = "dat"
    private const val MAX_FILE_NAME_LENGTH = 100
    private const val MAX_EXTENSION_LENGTH = 10

    fun sanitize(fileName: String, defaultName: String = DEFAULT_FILE_NAME): String {
        if (fileName.isBlank()) return defaultName

        // 1. Strip path separators, backslashes, and null bytes to extract basename
        val stripped = fileName
            .replace('\u0000', ' ')
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()

        if (stripped.isBlank() || stripped == "." || stripped == ".." || stripped.all { it == '.' || it == '_' }) {
            return defaultName
        }

        // 2. Separate base name and extension
        val lastDotIndex = stripped.lastIndexOf('.')
        val (rawBase, rawExt) = if (lastDotIndex > 0) {
            stripped.substring(0, lastDotIndex) to stripped.substring(lastDotIndex + 1)
        } else if (lastDotIndex == 0) {
            "" to stripped.substring(1)
        } else {
            stripped to ""
        }

        // 3. Sanitize base name (allow alphanumeric, underscore, hyphen, space-to-underscore)
        val sanitizedBase = rawBase
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim { it == '.' || it == '_' }

        // 4. Sanitize extension (alphanumeric only, bounded length)
        val sanitizedExt = rawExt
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .take(MAX_EXTENSION_LENGTH)

        val finalBase = sanitizedBase.ifBlank { DEFAULT_BASE_NAME }
        val finalExt = when {
            sanitizedExt.isNotBlank() -> sanitizedExt
            rawExt.isNotBlank() -> DEFAULT_EXTENSION
            lastDotIndex != -1 -> DEFAULT_EXTENSION
            else -> DEFAULT_EXTENSION
        }

        val truncatedBase = finalBase.take(MAX_FILE_NAME_LENGTH - finalExt.length - 1)
            .trimEnd { it == '.' || it == '_' }
            .ifBlank { DEFAULT_BASE_NAME }

        val result = "$truncatedBase.$finalExt"
        return if (result.isBlank() || result == "." || result == ".." || result.all { it == '.' || it == '_' }) {
            defaultName
        } else {
            result
        }
    }
}
