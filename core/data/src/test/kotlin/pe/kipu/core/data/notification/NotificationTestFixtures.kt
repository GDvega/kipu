package pe.kipu.core.data.notification

import java.io.BufferedReader

internal object NotificationTestFixtures {
    fun load(path: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing test resource: $path"
        }.bufferedReader().use(BufferedReader::readText)
}
