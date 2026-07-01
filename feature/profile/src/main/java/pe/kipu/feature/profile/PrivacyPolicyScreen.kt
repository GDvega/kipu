package pe.kipu.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuSubScreenScaffold

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    KipuSubScreenScaffold(
        title = "Política de privacidad",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KipuLayout.screenHorizontalPadding),
        ) {
            Text(
                text = "Cómo Kipu trata tus datos en el dispositivo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            privacySections.forEach { section ->
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Text(
                    text = section.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Última actualización: junio 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp),
            )
        }
    }
}

private data class PrivacySection(
    val title: String,
    val body: String,
)

private val privacySections = listOf(
    PrivacySection(
        title = "Resumen",
        body = "Kipu es una app de finanzas personales para Perú. Tus movimientos y " +
            "configuración se guardan en tu celular. No pedimos claves de Yape, Plin ni bancos, " +
            "y no subimos tus datos financieros a servidores propios.",
    ),
    PrivacySection(
        title = "Qué guardamos",
        body = "Movimientos, categorías, sobres, metas, cuentas compartidas y preferencias que tú registras " +
            "o confirmas. Si compartes un comprobante, la imagen se procesa en el dispositivo " +
            "con OCR local; no la enviamos a la nube por defecto.",
    ),
    PrivacySection(
        title = "Notificaciones (opcional)",
        body = "Si activas el acceso en Perfil, podemos leer notificaciones de ingresos de " +
            "Yape o Plin para sugerirte un movimiento. Siempre debes confirmar antes de guardar. " +
            "Puedes desactivar esta función cuando quieras.",
    ),
    PrivacySection(
        title = "Exportar y eliminar",
        body = "En Perfil puedes exportar una copia JSON o CSV de tus datos, o eliminar " +
            "todo con confirmación doble. Las exportaciones que compartes con otras apps " +
            "quedan bajo tu responsabilidad.",
    ),
    PrivacySection(
        title = "Copias de seguridad",
        body = "La base de datos financiera y preferencias sensibles están excluidas de la " +
            "copia automática de Google en la configuración actual de Kipu.",
    ),
    PrivacySection(
        title = "Contacto",
        body = "Consultas sobre privacidad: privacidad@kipu.pe (actualizar antes de publicar " +
            "en Play Store).",
    ),
)
