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
import pe.kipu.core.designsystem.component.kipuScrollbar

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
        val privacyScrollState = androidx.compose.foundation.rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .kipuScrollbar(privacyScrollState)
                .verticalScroll(privacyScrollState)
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
                text = "Última actualización: 25 de agosto de 2026",
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
            "y tus movimientos permanecen en el dispositivo. Si usas comandos de voz, Kipu " +
            "interpreta la transcripción localmente después del reconocimiento del sistema.",
    ),
    PrivacySection(
        title = "Qué guardamos",
        body = "Movimientos, categorías, sobres, metas, cuentas compartidas y preferencias que tú registras " +
            "o confirmas. Si compartes un comprobante, la imagen se procesa en el dispositivo " +
            "con OCR local y se lee desde su app de origen; no la enviamos a la nube por defecto. " +
            "Kipu no toma ni conserva fotos con la cámara.",
    ),
    PrivacySection(
        title = "Notificaciones (opcional)",
        body = "Si activas el acceso en Perfil, podemos leer notificaciones de ingresos de " +
            "Yape o Plin para sugerirte un movimiento. Siempre debes confirmar antes de guardar. " +
            "Puedes desactivar esta función cuando quieras.",
    ),
    PrivacySection(
        title = "Comandos de voz (opcional)",
        body = "Al usar el micrófono, el servicio de reconocimiento de voz del dispositivo " +
            "convierte tu voz en texto según las condiciones de su proveedor. Después, Kipu " +
            "interpreta esa transcripción en el dispositivo para proponer monto, categoría y " +
            "medio de pago. No enviamos la transcripción a servidores de Kipu ni guardamos el " +
            "resultado hasta que tú lo revisas y confirmas.",
    ),
    PrivacySection(
        title = "Métricas técnicas de ML Kit",
        body = "El OCR procesa la imagen y el texto dentro de tu celular: esos datos financieros " +
            "no se envían a Google. El SDK de ML Kit sí envía a Google métricas técnicas, como " +
            "información del dispositivo y la app, identificadores de instalación, rendimiento, " +
            "configuración, eventos y errores. Google las usa para diagnóstico y analítica, las " +
            "cifra durante el envío y declara que no las comparte con terceros.",
    ),
    PrivacySection(
        title = "Exportar y eliminar",
        body = "En Perfil puedes exportar todos tus datos en JSON o tus movimientos en CSV, o eliminar " +
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
