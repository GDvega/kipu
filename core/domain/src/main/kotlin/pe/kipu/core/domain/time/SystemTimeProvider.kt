package pe.kipu.core.domain.time

import java.time.Instant
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()
}
