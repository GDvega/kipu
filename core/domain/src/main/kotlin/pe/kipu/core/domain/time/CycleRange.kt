package pe.kipu.core.domain.time

import java.time.Instant

/**
 * A half-open time range `[start, end)` in UTC instants.
 */
data class CycleRange(
    val start: Instant,
    val end: Instant,
) {
    init {
        require(!end.isBefore(start)) { "Cycle range end must not be before start" }
    }

    fun contains(instant: Instant): Boolean =
        !instant.isBefore(start) && instant.isBefore(end)
}
