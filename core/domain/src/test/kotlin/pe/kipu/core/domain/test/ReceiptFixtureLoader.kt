package pe.kipu.core.domain.test

object ReceiptFixtureLoader {
    fun load(resourceName: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(resourceName)) {
            "Missing fixture: $resourceName"
        }.bufferedReader().use { it.readText() }
}
