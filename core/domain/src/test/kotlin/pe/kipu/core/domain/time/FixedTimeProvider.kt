package pe.kipu.core.domain.time

import java.time.Instant

class FixedTimeProvider(
    private val instant: Instant,
) : TimeProvider {
    override fun now(): Instant = instant
}
