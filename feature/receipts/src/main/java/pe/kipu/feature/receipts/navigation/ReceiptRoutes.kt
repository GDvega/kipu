package pe.kipu.feature.receipts.navigation

object ReceiptRoutes {
    const val HUB = "receipts"
    const val REVIEW = "receipts/review/{contentUri}"
    const val CONTENT_URI_ARG = "contentUri"

    fun review(contentUri: String): String = "receipts/review/${encode(contentUri)}"

    fun decodeContentUri(encoded: String): String = java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
