package pe.kipu.core.domain.export

enum class ExportFormat(
    val fileExtension: String,
    val mimeType: String,
    val csvDelimiter: Char? = null,
) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv", ','),
    /** Excel en español (Perú/LATAM) abre correctamente con punto y coma. */
    CSV_EXCEL_PE("csv", "text/csv", ';'),
}
