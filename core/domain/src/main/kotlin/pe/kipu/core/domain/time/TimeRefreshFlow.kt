package pe.kipu.core.domain.time

import java.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

object TimeRefreshConfig {
    const val DEFAULT_INTERVAL_MS: Long = 60_000L
}

fun TimeProvider.refreshTicks(intervalMs: Long = TimeRefreshConfig.DEFAULT_INTERVAL_MS): Flow<Instant> = flow {
    emit(now())
    while (currentCoroutineContext().isActive) {
        delay(intervalMs)
        emit(now())
    }
}
