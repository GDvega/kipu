package pe.kipu.core.domain.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

object FlowFirstDefaults {
    const val TIMEOUT_MS: Long = 5_000L
}

/**
 * Returns the first emission from [this] flow, or [default] if nothing arrives within [timeoutMs].
 * Guards against hung DataStore/Room collectors on slow devices.
 */
suspend fun <T> Flow<T>.firstWithTimeout(
    default: T,
    timeoutMs: Long = FlowFirstDefaults.TIMEOUT_MS,
): T = withTimeoutOrNull(timeoutMs) { first() } ?: default
