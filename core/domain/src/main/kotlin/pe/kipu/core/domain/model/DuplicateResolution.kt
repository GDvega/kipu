package pe.kipu.core.domain.model

/**
 * User decision when a possible duplicate movement is detected (future phase).
 */
enum class DuplicateResolution {
    MERGE,
    SAVE_AS_NEW,
    CANCEL,
}
