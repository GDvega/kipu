package pe.kipu.core.domain.model

/**
 * In-memory receipt image for OCR without exposing Android Bitmap to domain.
 */
data class OcrImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
) {
    init {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        require(rotationDegrees in setOf(0, 90, 180, 270)) {
            "Rotation must be 0, 90, 180 or 270 degrees"
        }
        require(bytes.size <= MAX_BYTES) {
            "Image exceeds maximum allowed size"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OcrImage
        return bytes.contentEquals(other.bytes) &&
            width == other.width &&
            height == other.height &&
            rotationDegrees == other.rotationDegrees
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rotationDegrees
        return result
    }

    companion object {
        const val MAX_BYTES: Int = 10 * 1024 * 1024
    }
}
