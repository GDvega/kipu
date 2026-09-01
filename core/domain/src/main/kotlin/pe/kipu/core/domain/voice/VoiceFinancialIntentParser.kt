package pe.kipu.core.domain.voice

import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.receipt.ServiceReceiptKey
import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

import javax.inject.Inject

class VoiceFinancialIntentParser @Inject constructor() {

    fun parse(rawText: String): VoiceFinancialIntent {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return VoiceFinancialIntent.Unknown(rawText)
        }

        val normalized = normalize(trimmed)

        // 1. Detect Goal Contribution
        parseGoalContribution(trimmed, normalized)?.let { return it }

        // 2. Detect Service Receipt Payment
        parseServiceReceiptPayment(trimmed, normalized)?.let { return it }

        // 3. Detect Income
        parseIncome(trimmed, normalized)?.let { return it }

        // 4. Detect Expense
        parseExpense(trimmed, normalized)?.let { return it }

        return VoiceFinancialIntent.Unknown(trimmed)
    }

    private fun parseGoalContribution(rawText: String, normalized: String): VoiceFinancialIntent.GoalContribution? {
        val goalPatterns = listOf(
            Pattern.compile("^(?:he\\s+)?(?:guardado|ahorrado|abonado|metido|puesto)\\s+(.+?)\\s+(?:para|a|en)\\s+(?:mi\\s+)?(?:meta\\s+(?:de\\s+)?)?(.+)$"),
            Pattern.compile("^(?:guarde|ahorre|abone|meti|puse)\\s+(.+?)\\s+(?:para|a|en)\\s+(?:mi\\s+)?(?:meta\\s+(?:de\\s+)?)?(.+)$"),
            Pattern.compile("^(?:abono|ahorro)\\s+(?:de\\s+)?(.+?)\\s+(?:para|a|en)\\s+(?:mi\\s+)?(?:meta\\s+(?:de\\s+)?)?(.+)$"),
        )

        for (pattern in goalPatterns) {
            val matcher = pattern.matcher(normalized)
            if (matcher.matches()) {
                val amountStr = matcher.group(1).trim()
                val goalTarget = matcher.group(2).trim()
                val amount = extractMoney(amountStr) ?: continue
                val cleanGoalName = cleanGoalName(goalTarget)
                return VoiceFinancialIntent.GoalContribution(
                    rawText = rawText,
                    amount = amount,
                    goalQuery = cleanGoalName,
                    description = "Abono a meta $cleanGoalName",
                )
            }
        }
        return null
    }

    private fun parseServiceReceiptPayment(rawText: String, normalized: String): VoiceFinancialIntent.ServiceReceiptPayment? {
        val amountFirstPattern = Pattern.compile(
            "^(?:ya\\s+)?(?:he\\s+)?(?:pagado|cancelado|pague|cancele)\\s+(.+?)\\s+(?:de|del|por)\\s+(?:el\\s+)?(?:recibo\\s+(?:de|del)\\s+)?(luz|agua|gas|internet|celular|telefono|alquiler|casa|deuda|prestamo|universidad|colegio|pension)$",
        )
        val amountFirstMatch = amountFirstPattern.matcher(normalized)
        if (amountFirstMatch.matches()) {
            val amount = extractMoney(amountFirstMatch.group(1))
            val serviceKey = mapServiceKey(amountFirstMatch.group(2))
            if (amount != null && serviceKey != null) {
                return VoiceFinancialIntent.ServiceReceiptPayment(
                    rawText = rawText,
                    serviceKey = serviceKey,
                    amount = amount,
                    description = "Pago de ${serviceKey.defaultTitle}",
                )
            }
        }

        val receiptPatterns = listOf(
            Pattern.compile("^(?:ya\\s+)?(?:he\\s+)?(?:pagado|cancelado)\\s+(?:mi\\s+|el\\s+)?(?:recibo\\s+(?:de\\s+|del\\s+)?)?(luz|agua|gas|internet|celular|telefono|alquiler|casa|deuda|prestamo|universidad|colegio|pension)(?:\\s+(?:de\\s+)?(.+))?$"),
            Pattern.compile("^(?:ya\\s+)?(?:pague|cancele)\\s+(?:mi\\s+|el\\s+|la\\s+)?(?:recibo\\s+(?:de\\s+|del\\s+)?)?(luz|agua|gas|internet|celular|telefono|alquiler|casa|deuda|prestamo|universidad|colegio|pension)(?:\\s+(?:de\\s+)?(.+))?$"),
            Pattern.compile("^(?:recibo\\s+(?:de\\s+|del\\s+)?)(luz|agua|gas|internet|celular|telefono|alquiler|casa|deuda|prestamo|universidad|colegio|pension)(?:\\s+(?:pagado|cancelado))?(?:\\s+(?:de\\s+)?(.+))?$"),
        )

        for (pattern in receiptPatterns) {
            val matcher = pattern.matcher(normalized)
            if (matcher.matches()) {
                val serviceStr = matcher.group(1).trim()
                val amountStr = matcher.group(2)?.trim()
                val serviceKey = mapServiceKey(serviceStr) ?: continue
                val amount = amountStr?.let { extractMoney(it) }
                val title = serviceKey.defaultTitle
                return VoiceFinancialIntent.ServiceReceiptPayment(
                    rawText = rawText,
                    serviceKey = serviceKey,
                    amount = amount,
                    description = "Pago de $title",
                )
            }
        }
        return null
    }

    private fun parseIncome(rawText: String, normalized: String): VoiceFinancialIntent.Income? {
        val incomePatterns = listOf(
            Pattern.compile("^(?:me\\s+pagaron|he\\s+cobrado|cobre|ingreso\\s+de|me\\s+depositaron|he\\s+ganado|gane|recibi|me\\s+transfirieron)\\s+(.+?)(?:\\s+(?:por|de|en|de\\s+la|del)\\s+(.+))?$"),
        )

        for (pattern in incomePatterns) {
            val matcher = pattern.matcher(normalized)
            if (matcher.matches()) {
                val amountStr = matcher.group(1).trim()
                val sourceStr = matcher.group(2)?.trim().orEmpty()
                val amount = extractMoney(amountStr) ?: continue
                val channel = extractChannel(normalized)
                val description = if (sourceStr.isNotBlank()) {
                    sourceStr.replaceFirstChar { it.uppercase() }
                } else {
                    "Ingreso"
                }
                return VoiceFinancialIntent.Income(
                    rawText = rawText,
                    amount = amount,
                    categoryId = CategoryIds.OTHER,
                    description = description,
                    channel = channel,
                )
            }
        }
        return null
    }

    private fun parseExpense(rawText: String, normalized: String): VoiceFinancialIntent.Expense? {
        val channel = extractChannel(normalized)
		val expenseText = normalized.replaceFirst(
			Regex("^(?:ya\\s+pe|yape|yapee|yapie)\\s+"),
			"pague ",
		)

        val expensePatterns = listOf(
            // "he pagado un taxi 15 soles" / "gaste en almuerzo 20 soles" / "compre agua 3 soles"
            Pattern.compile("^(?:he\\s+gastado|gaste|he\\s+comprado|compre|he\\s+pagado|pague|consumi)\\s+(?:un\\s+|una\\s+|el\\s+|la\\s+)?([a-zA-ZáéíóúñÁÉÍÓÚÑ\\s]+?)\\s+(?:de\\s+|por\\s+|a\\s+)?(\\d+.*)$"),
            // "he gastado 5 soles comprandome un agua" / "gaste 10 soles en un taxi"
            Pattern.compile("^(?:he\\s+gastado|gaste|he\\s+comprado|compre|he\\s+pagado|pague|consumi)\\s+(.+?)\\s+(?:comprandome|comprando|en\\s+un|en\\s+una|en|para\\s+pagar\\s+un|para\\s+pagar\\s+una|para\\s+un|para\\s+una|para|de|un|una)\\s+(.+)$"),
            // "pague 15 soles de taxi"
            Pattern.compile("^(?:he\\s+gastado|gaste|he\\s+pagado|pague)\\s+(.+?)\\s+(?:de|por)\\s+(.+)$"),
            // "compre un agua por 3 soles"
            Pattern.compile("^(?:he\\s+comprado|compre)\\s+(.+?)\\s+(?:por|a|en|de)\\s+(.+)$"),
            // "gasto hormiga 5 soles" / "hormiga 3 soles"
            Pattern.compile("^(?:gasto\\s+hormiga|gastos\\s+hormiga|hormiga)\\s+(?:de\\s+|por\\s+)?(.+)$"),
            // "pasaje 2.50" / "taxi de 15 soles" / "menu 10 soles" / "transporte 5 soles"
            Pattern.compile("^(transporte|pasaje|pasajes|taxi|movilidad|comida|almuerzo|menu|desayuno|cena|agua|gaseosa|cafe|galleta|galletas|pan|chifa|ocio|servicios?|farmacia|ropa)\\s+(?:de\\s+|por\\s+)?(.+)$"),
            // "gaste 5 soles" / "compre 10 soles" / "pague 20 soles" (monto directo)
            Pattern.compile("^(?:he\\s+gastado|gaste|he\\s+comprado|compre|he\\s+pagado|pague|consumi)\\s+(.+)$"),
        )

        for (pattern in expensePatterns) {
			val matcher = pattern.matcher(expenseText)
            if (matcher.matches()) {
                val groupCount = matcher.groupCount()
                if (groupCount == 1) {
                    val single = matcher.group(1).trim()
                    val money = extractMoney(single)
                    if (money != null) {
                        return VoiceFinancialIntent.Expense(
                            rawText = rawText,
                            amount = money,
                            categoryId = CategoryIds.OTHER,
                            description = "Gasto general",
                            channel = channel,
                        )
                    }
                } else if (groupCount >= 2) {
                    var first = matcher.group(1).trim()
                    var second = matcher.group(2).trim()

                    var money = extractMoney(first)
                    var desc = second
                    if (money == null) {
                        money = extractMoney(second)
                        desc = first
                    }

                    if (money != null) {
                        val cleanDesc = cleanExpenseDescription(desc)
                        val categoryId = inferCategory(cleanDesc)
                        val matchedService = inferServiceKey(cleanDesc)
                        return VoiceFinancialIntent.Expense(
                            rawText = rawText,
                            amount = money,
                            categoryId = categoryId,
                            description = cleanDesc.ifBlank { "Gasto" }.replaceFirstChar { it.uppercase() },
                            channel = channel,
                            matchedServiceKey = matchedService,
                        )
                    }
                }
            }
        }

        // Direct pattern: "[Monto] en/de/para [Descripción]" (e.g. "5 soles en transporte", "10 lucas de pasaje", "3 soles en gasto hormiga")
        val directPattern = Pattern.compile("^(.+?)\\s+(?:en\\s+un|en\\s+una|en|de|para\\s+un|para\\s+una|para)\\s+(.+)$")
		val directMatcher = directPattern.matcher(expenseText)
        if (directMatcher.matches()) {
            val first = directMatcher.group(1).trim()
            val second = directMatcher.group(2).trim()
            var money = extractMoney(first)
            var desc = second
            if (money == null) {
                money = extractMoney(second)
                desc = first
            }
            if (money != null) {
                val cleanDesc = cleanExpenseDescription(desc)
                val categoryId = inferCategory(cleanDesc)
                val matchedService = inferServiceKey(cleanDesc)
                return VoiceFinancialIntent.Expense(
                    rawText = rawText,
                    amount = money,
                    categoryId = categoryId,
                    description = cleanDesc.ifBlank { "Gasto" }.replaceFirstChar { it.uppercase() },
                    channel = channel,
                    matchedServiceKey = matchedService,
                )
            }
        }

        // Check if raw text is just an amount (e.g. "5 soles", "10 lucas", "5.50")
		extractMoney(expenseText)?.let { money ->
            return VoiceFinancialIntent.Expense(
                rawText = rawText,
                amount = money,
                categoryId = CategoryIds.OTHER,
                description = "Gasto general",
                channel = channel,
            )
        }

        return null
    }

    private fun extractChannel(text: String): PaymentChannel {
        return when {
			text.contains("yape") || text.contains("yapie") || text.contains("ya pe") -> PaymentChannel.YAPE
            text.contains("plin") -> PaymentChannel.PLIN
            text.contains("tarjeta") || text.contains("banco") || text.contains("transferencia") -> PaymentChannel.OTHER
            else -> PaymentChannel.CASH
        }
    }

    private fun inferCategory(description: String): String {
        val lower = description.lowercase()
        return when {
            // 1. Ant Spending / Hormiga / Gustitos
            ANT_SPENDING_KEYWORDS.any { lower.contains(it) } -> CategoryIds.OTHER
            // 2. Transporte / Movilidad
            TRANSPORT_KEYWORDS.any { lower.contains(it) } -> CategoryIds.TRANSPORT
            // 3. Servicios / Recibos
            SERVICES_KEYWORDS.any { lower.contains(it) } -> CategoryIds.SERVICES
            // 4. Ocio / Salud / Educación / Hogar -> CategoryIds.OTHER
            LEISURE_KEYWORDS.any { lower.contains(it) } -> CategoryIds.OTHER
            HEALTH_KEYWORDS.any { lower.contains(it) } -> CategoryIds.OTHER
            EDUCATION_KEYWORDS.any { lower.contains(it) } -> CategoryIds.OTHER
            PERSONAL_HOME_KEYWORDS.any { lower.contains(it) } -> CategoryIds.OTHER
            // 5. Comida / Alimentos
            FOOD_KEYWORDS.any { lower.contains(it) } -> CategoryIds.FOOD
            // Default to OTHER for general/unknown purchases instead of forcing to FOOD
            else -> CategoryIds.OTHER
        }
    }

    private fun inferServiceKey(description: String): ServiceReceiptKey? {
        val lower = description.lowercase()
        return when {
            lower.contains("luz") -> ServiceReceiptKey.LIGHT
            lower.contains("agua") && (lower.contains("recibo") || lower.contains("sedapal")) -> ServiceReceiptKey.WATER
            lower.contains("internet") || lower.contains("wifi") -> ServiceReceiptKey.INTERNET
            lower.contains("celular") || lower.contains("telefono") || lower.contains("plan") -> ServiceReceiptKey.PHONE
            lower.contains("alquiler") || lower.contains("renta") -> ServiceReceiptKey.RENT
            lower.contains("prestamo") || lower.contains("deuda") -> ServiceReceiptKey.DEBTS
            lower.contains("universidad") || lower.contains("colegio") || lower.contains("pension") -> ServiceReceiptKey.EDUCATION
            else -> null
        }
    }

    private fun mapServiceKey(serviceStr: String): ServiceReceiptKey? {
        val lower = serviceStr.lowercase()
        return when {
            lower.contains("luz") -> ServiceReceiptKey.LIGHT
            lower.contains("agua") -> ServiceReceiptKey.WATER
            lower == "gas" -> ServiceReceiptKey.GAS
            lower.contains("internet") -> ServiceReceiptKey.INTERNET
            lower.contains("celular") || lower.contains("telefono") -> ServiceReceiptKey.PHONE
            lower.contains("alquiler") || lower.contains("casa") -> ServiceReceiptKey.RENT
            lower.contains("prestamo") || lower.contains("deuda") -> ServiceReceiptKey.DEBTS
            lower.contains("universidad") || lower.contains("colegio") || lower.contains("pension") -> ServiceReceiptKey.EDUCATION
            else -> null
        }
    }

    private fun cleanGoalName(raw: String): String {
        var clean = raw.trim()
        clean = clean.removePrefix("mi ").removePrefix("la ").removePrefix("el ")
        clean = clean.removePrefix("meta ").removePrefix("de ").removePrefix("para ")
        return clean.trim().replaceFirstChar { it.uppercase() }
    }

    private fun cleanExpenseDescription(raw: String): String {
        var clean = raw.trim()
        clean = clean.removePrefix("un ").removePrefix("una ").removePrefix("el ").removePrefix("la ")
        clean = clean.removePrefix("en un ").removePrefix("en una ").removePrefix("en ")
        clean = clean.removePrefix("de un ").removePrefix("de una ").removePrefix("de ")
        clean = clean.removePrefix("por un ").removePrefix("por una ").removePrefix("por ")
        clean = clean.removePrefix("para un ").removePrefix("para una ").removePrefix("para ")
        clean = clean.removePrefix("pagar un ").removePrefix("pagar una ").removePrefix("pagar ")
        clean = clean.removePrefix("comprar un ").removePrefix("comprar una ").removePrefix("comprar ")
        clean = clean.removePrefix("comprarme un ").removePrefix("comprarme una ").removePrefix("comprarme ")
        clean = clean.removeSuffix(" con yape").removeSuffix(" por yape")
        clean = clean.removeSuffix(" con plin").removeSuffix(" por plin")
        clean = clean.removeSuffix(" en efectivo")
        return clean.trim()
    }

    private fun extractMoney(input: String): Money? {
        val lower = input.lowercase().trim()

        // Match digits e.g. "5", "5.50", "5,50", "S/ 5.00", "5 soles", "10 lucas", "1 luca"
        val regex = Regex("""(?:s/\.?\s*)?(\d+(?:[.,]\d{1,2})?)\s*(?:soles?|lucas?|luca)?""")
        val match = regex.find(lower)
        if (match != null) {
            val numStr = match.groupValues[1].replace(',', '.')
            val bd = numStr.toBigDecimalOrNull()
            if (bd != null && bd > BigDecimal.ZERO) {
                return when (val res = Money.of(bd)) {
                    is pe.kipu.core.domain.model.DomainResult.Ok -> res.value
                    is pe.kipu.core.domain.model.DomainResult.Err -> null
                }
            }
        }

        // Match Spanish number words
        val wordAmount = parseSpanishNumberWords(lower)
        if (wordAmount != null && wordAmount > BigDecimal.ZERO) {
            return when (val res = Money.of(wordAmount)) {
                is pe.kipu.core.domain.model.DomainResult.Ok -> res.value
                is pe.kipu.core.domain.model.DomainResult.Err -> null
            }
        }

        return null
    }

    private fun parseSpanishNumberWords(text: String): BigDecimal? {
        val clean = text.replace("soles", "").replace("sol", "").replace("lucas", "").replace("luca", "").trim()
        return when (clean) {
            "un", "uno" -> BigDecimal("1")
            "dos" -> BigDecimal("2")
            "tres" -> BigDecimal("3")
            "cuatro" -> BigDecimal("4")
            "cinco" -> BigDecimal("5")
            "seis" -> BigDecimal("6")
            "siete" -> BigDecimal("7")
            "ocho" -> BigDecimal("8")
            "nueve" -> BigDecimal("9")
            "diez" -> BigDecimal("10")
            "once" -> BigDecimal("11")
            "doce" -> BigDecimal("12")
            "trece" -> BigDecimal("13")
            "catorce" -> BigDecimal("14")
            "quince" -> BigDecimal("15")
            "dieciseis", "dieciséis" -> BigDecimal("16")
            "diecisiete" -> BigDecimal("17")
            "dieciocho" -> BigDecimal("18")
            "diecinueve" -> BigDecimal("19")
            "veinte" -> BigDecimal("20")
            "veinticinco" -> BigDecimal("25")
            "treinta" -> BigDecimal("30")
            "cuarenta" -> BigDecimal("40")
            "cincuenta" -> BigDecimal("50")
            "sesenta" -> BigDecimal("60")
            "setenta" -> BigDecimal("70")
            "ochenta" -> BigDecimal("80")
            "noventa" -> BigDecimal("90")
            "cien", "ciento" -> BigDecimal("100")
            "doscientos" -> BigDecimal("200")
            "trescientos" -> BigDecimal("300")
            "cuatrocientos" -> BigDecimal("400")
            "quinientos" -> BigDecimal("500")
            "mil" -> BigDecimal("1000")
            "dos mil" -> BigDecimal("2000")
            else -> null
        }
    }

    private fun normalize(input: String): String {
        val nfd = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val stripped = nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return stripped
            .replace(Regex("""[¿?¡!;,]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private companion object {
        val ANT_SPENDING_KEYWORDS = listOf(
            "hormiga", "gasto hormiga", "gastos hormiga", "antojo", "antojito", "antojitos",
            "gustito", "gustitos", "chucheria", "chucherias", "chatarra", "cigarro", "cigarros",
            "vape", "golosina", "golosinas", "chicle", "chicles", "caramelo", "caramelos",
        )

        val TRANSPORT_KEYWORDS = listOf(
            "transporte", "pasaje", "pasajes", "pasajito", "movilidad", "taxi", "taxis",
            "bus", "buses", "micro", "micros", "corredor", "metropolitano", "gasolina",
            "combustible", "gnv", "glp", "colectivo", "colectivos", "uber", "didi", "cabify",
            "indrive", "tren", "metro", "combi", "combis", "moto", "mototaxi",
            "estacionamiento", "cochera", "peaje", "peajes", "viaje", "vuelo",
        )

        val SERVICES_KEYWORDS = listOf(
			"servicio", "servicios", "recibo", "recibos", "luz", "agua potable", "sedapal",
            "enel", "luz del sur", "electrocentro", "internet", "wifi", "claro", "movistar",
            "entel", "bitel", "celular", "cable", "telefono", "gas", "calidda",
            "alquiler", "renta", "mantenimiento", "condominio", "arbitrios", "predial",
        )

        val LEISURE_KEYWORDS = listOf(
            "ocio", "entretenimiento", "diversion", "cine", "pelicula", "netflix", "spotify",
            "youtube", "disney", "hbo", "prime", "juego", "videojuego", "fiesta", "salida",
            "salidas", "bar", "discoteca", "cerveza", "cervezas", "trago", "tragos",
            "chela", "chelas", "concierto", "evento", "paseo",
        )

        val HEALTH_KEYWORDS = listOf(
            "farmacia", "botica", "inkafarma", "mifarma", "medicina", "medicinas", "medicamento",
            "medicamentos", "pastilla", "pastillas", "doctor", "medico", "consulta", "clinica",
            "hospital", "dentista", "odontologo", "lentes",
        )

        val EDUCATION_KEYWORDS = listOf(
            "educacion", "universidad", "instituto", "colegio", "pension", "matricula",
            "cuaderno", "cuadernos", "libro", "libros", "utiles", "curso", "clases", "taller",
        )

        val PERSONAL_HOME_KEYWORDS = listOf(
            "ropa", "zapato", "zapatos", "zapatillas", "pantalon", "polo", "camisa", "vestido",
            "peluqueria", "corte", "barberia", "salon", "familia", "casa", "hogar", "limpieza",
            "mueble", "reparacion", "regalo", "mascota", "veterinaria", "ferreteria", "decoracion",
            "compras", "compra", "otro", "otros", "varios",
        )

        val FOOD_KEYWORDS = listOf(
			"comida", "alimento", "alimentos", "agua", "gaseosa", "bebida", "almuerzo", "menu", "desayuno", "cena", "chifa",
            "pan", "panaderia", "galleta", "galletas", "cafe", "hamburguesa", "pollo", "pollo a la brasa",
            "snack", "torta", "fruta", "frutas", "verdura", "verduras", "jugo", "helado",
            "ceviche", "carne", "arroz", "aceite", "leche", "huevos", "abarrotes", "mercado",
            "supermercado", "bodega", "tienda", "tambo", "oxxo", "kfc", "bembos", "piqueo",
            "restaurant", "restaurante", "pizza", "chicharron", "sopa", "caldo",
        )
    }
}
