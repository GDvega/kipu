package pe.kipu.feature.receipts

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ReceiptCaptureUriFactory {

    fun create(context: Context): Uri {
        val directory = File(context.cacheDir, RECEIPTS_CACHE_DIR).apply { mkdirs() }
        val file = File(directory, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    private const val RECEIPTS_CACHE_DIR = "receipts"
}
