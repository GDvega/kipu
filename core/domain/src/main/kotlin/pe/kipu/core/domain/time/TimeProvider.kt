package pe.kipu.core.domain.time

import java.time.Instant

/**
 * Injectable clock for deterministic domain calculations and tests.
 */
fun interface TimeProvider {
    fun now(): Instant
}
