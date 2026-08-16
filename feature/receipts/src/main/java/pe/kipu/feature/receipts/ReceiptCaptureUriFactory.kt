package pe.kipu.feature.receipts

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object ReceiptCaptureUriFactory {

    fun create(context: Context): Uri {
        val directory = File(context.cacheDir, RECEIPTS_CACHE_DIR).apply { mkdirs() }
        val file = File(directory, "capture-${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun deleteIfOwnedCapture(context: Context, contentUri: String): Boolean {
        val uri = Uri.parse(contentUri)
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return false
        if (uri.authority != "${context.packageName}.fileprovider") return false
        val pathSegments = uri.pathSegments
        if (pathSegments.size != 2 || pathSegments.first() != RECEIPTS_CACHE_DIR) return false

        val fileName = pathSegments.last()
        if (!CAPTURE_FILE_NAME.matches(fileName)) return false

        return File(File(context.cacheDir, RECEIPTS_CACHE_DIR), fileName).delete()
    }

    private const val RECEIPTS_CACHE_DIR = "receipts"
    private val CAPTURE_FILE_NAME = Regex(
        "capture-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg",
    )
}
