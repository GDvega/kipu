package pe.kipu.core.data.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/** Emits [default] before the upstream collector starts so `combine` never blocks forever. */
fun <T> Flow<T>.withImmediateDefault(default: T): Flow<T> = onStart { emit(default) }
