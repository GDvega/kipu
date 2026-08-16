package pe.kipu.app.receipt

import android.content.Intent
import android.net.Uri
import android.os.Build

object ReceiptShareIntentParser {

    private val REJECTED_MIME_TYPES = setOf(
        "image/svg+xml",
        "image/gif",
        "image/vnd.wap.wbmp",
    )

    fun extractImageUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null
        if (!isSupportedMimeType(type)) return null

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return uri?.takeIf { isSafeContentUri(it) }
    }

    internal fun isSupportedMimeType(mimeType: String): Boolean {
        val normalized = mimeType.trim().lowercase()
        if (!normalized.startsWith("image/")) return false
        if (normalized in REJECTED_MIME_TYPES) return false
        return true
    }

    internal fun isSafeContentUri(uri: Uri): Boolean =
        isSafeContentUri(uri.scheme, uri.authority, uri.path)

    internal fun isSafeContentUri(scheme: String?, authority: String?, path: String?): Boolean {
        if (scheme != "content") return false
        if (authority.isNullOrBlank()) return false
        if (path.isNullOrBlank()) return false
        return true
    }
}
