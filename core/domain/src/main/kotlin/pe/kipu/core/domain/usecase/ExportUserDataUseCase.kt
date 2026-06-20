package pe.kipu.core.domain.usecase

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import pe.kipu.core.domain.export.ExportFormat
import pe.kipu.core.domain.export.UserDataCsvSerializer
import pe.kipu.core.domain.export.UserDataExportPayload
import pe.kipu.core.domain.export.UserDataJsonSerializer

class ExportUserDataUseCase @Inject constructor(
    private val buildUserDataSnapshot: BuildUserDataSnapshotUseCase,
    private val jsonSerializer: UserDataJsonSerializer,
    private val csvSerializer: UserDataCsvSerializer,
) {
    suspend operator fun invoke(format: ExportFormat): Result<UserDataExportPayload> = runCatching {
        val snapshot = buildUserDataSnapshot()
        val exportedOn = snapshot.exportedAt.atZone(ZoneId.of("America/Lima")).format(FILE_DATE_FORMATTER)
        when (format) {
            ExportFormat.JSON -> UserDataExportPayload(
                content = jsonSerializer.serialize(snapshot),
                format = format,
                fileName = "kipu_export_$exportedOn.json",
            )

            ExportFormat.CSV,
            ExportFormat.CSV_EXCEL_PE,
            -> {
                val delimiter = format.csvDelimiter ?: ','
                UserDataExportPayload(
                    content = csvSerializer.serializeMovements(snapshot, delimiter),
                    format = format,
                    fileName = when (format) {
                        ExportFormat.CSV_EXCEL_PE -> "kipu_export_${exportedOn}_movimientos_excel.csv"
                        else -> "kipu_export_${exportedOn}_movimientos.csv"
                    },
                )
            }
        }
    }

    private companion object {
        val FILE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
