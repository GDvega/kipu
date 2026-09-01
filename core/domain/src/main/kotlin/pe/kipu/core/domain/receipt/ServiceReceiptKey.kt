package pe.kipu.core.domain.receipt

enum class ServiceReceiptType {
    LIGHT,
    WATER,
    INTERNET,
    PHONE,
    RENT,
    DEBTS,
    EDUCATION,
    CUSTOM,
}

data class ServiceReceiptKey(
    val type: ServiceReceiptType,
    val customName: String? = null,
) {
    val identifier: String
        get() = when (type) {
            ServiceReceiptType.CUSTOM -> "CUSTOM_${customName?.trim()?.uppercase() ?: "EXTRA"}"
            else -> type.name
        }

    val defaultTitle: String
        get() = when (type) {
            ServiceReceiptType.LIGHT -> "Luz"
            ServiceReceiptType.WATER -> "Agua"
            ServiceReceiptType.INTERNET -> "Internet"
            ServiceReceiptType.PHONE -> "Celular"
            ServiceReceiptType.RENT -> "Alquiler / Casa"
            ServiceReceiptType.DEBTS -> "Préstamos y deudas"
            ServiceReceiptType.EDUCATION -> "Educación"
            ServiceReceiptType.CUSTOM -> customName?.takeIf { it.isNotBlank() } ?: "Servicio"
        }

    companion object {
        val LIGHT = ServiceReceiptKey(ServiceReceiptType.LIGHT)
        val WATER = ServiceReceiptKey(ServiceReceiptType.WATER)
        val INTERNET = ServiceReceiptKey(ServiceReceiptType.INTERNET)
        val GAS = custom("Gas")
        val PHONE = ServiceReceiptKey(ServiceReceiptType.PHONE)
        val RENT = ServiceReceiptKey(ServiceReceiptType.RENT)
        val DEBTS = ServiceReceiptKey(ServiceReceiptType.DEBTS)
        val EDUCATION = ServiceReceiptKey(ServiceReceiptType.EDUCATION)

        fun custom(name: String) = ServiceReceiptKey(ServiceReceiptType.CUSTOM, name.trim())

        fun fromIdentifier(id: String): ServiceReceiptKey {
            return when {
                id.startsWith("CUSTOM_") -> ServiceReceiptKey(
                    ServiceReceiptType.CUSTOM,
                    id.removePrefix("CUSTOM_").lowercase().replaceFirstChar { it.uppercase() },
                )
                id == "LIGHT" -> LIGHT
                id == "WATER" -> WATER
                id == "INTERNET" -> INTERNET
                id == "PHONE" -> PHONE
                id == "RENT" -> RENT
                id == "DEBTS" -> DEBTS
                id == "EDUCATION" -> EDUCATION
                else -> ServiceReceiptKey(ServiceReceiptType.CUSTOM, id)
            }
        }
    }
}
