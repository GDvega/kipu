package pe.kipu.core.domain.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowFirstWithTimeoutTest {

    @Test
    fun firstWithTimeout_returnsFirstValueWhenEmittedInTime() = runTest {
        val result = flow {
            emit(42)
        }.firstWithTimeout(default = 0, timeoutMs = 1_000)

        assertEquals(42, result)
    }

    @Test
    fun firstWithTimeout_returnsDefaultWhenTimedOut() = runTest {
        val result = flow {
            delay(10_000)
            emit(42)
        }.firstWithTimeout(default = 7, timeoutMs = 50)

        assertEquals(7, result)
    }
}
