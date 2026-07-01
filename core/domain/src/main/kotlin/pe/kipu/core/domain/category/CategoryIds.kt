package pe.kipu.core.domain.category

/**
 * Stable category ids for domain logic and Room seed data.
 */
object CategoryIds {
    const val FOOD: String = "category-food"
    const val TRANSPORT: String = "category-transport"
    const val SERVICES: String = "category-services"
    const val OTHER: String = "category-other"

    private val BUILT_IN_IDS: Set<String> = setOf(FOOD, TRANSPORT, SERVICES, OTHER)

    fun isBuiltIn(id: String): Boolean = id in BUILT_IN_IDS
}
