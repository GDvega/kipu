package pe.kipu.feature.onboarding.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuCardStyle
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuScreenHeader
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuPrimary

@Composable
fun PlanIntroStep(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        KipuScreenHeader(
            title = "Arma tu plan",
            subtitle = "Para calcular cuánto puedes gastar sin descuadrarte, Kipu necesita conocer tus ingresos, gastos principales y metas.",
        )
        Column(modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding)) {
            KipuCard(modifier = Modifier.fillMaxWidth(), style = KipuCardStyle.Large) {
                Text(
                    text = "¿Qué necesitamos?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                PlanCheckItem("Tus ingresos (fijos o variables)")
                PlanCheckItem("Tus gastos fijos obligatorios")
                PlanCheckItem("Cuánto quieres gastar por semana")
                PlanCheckItem("Tus metas de ahorro")
            }
            KipuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KipuLayout.sectionSpacing),
                style = KipuCardStyle.Large,
            ) {
                RowWithIcon(
                    icon = { Icon(Icons.Filled.Star, contentDescription = null, tint = KipuAmber) },
                    title = "Tiempo estimado",
                    body = "Menos de 2 minutos para la versión rápida. Puedes completar los detalles después desde tu perfil.",
                )
            }
            KipuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KipuLayout.sectionSpacing),
                style = KipuCardStyle.Large,
            ) {
                Text(
                    text = "Tu plan, tus números",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Configuramos ingresos y gastos fijos para calcular cuánto puedes gastar por semana. " +
                        "Siempre puedes ajustarlo después desde Sobres o Perfil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            KipuPrimaryButton(
                text = "Comenzar con mi plan",
                onClick = onStart,
                modifier = Modifier.padding(top = 32.dp),
            )
            KipuSecondaryButton(
                text = "Configurar plan después",
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = KipuLayout.screenHorizontalPadding),
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun PlanCheckItem(text: String) {
    RowWithIcon(
        icon = { Icon(Icons.Filled.Check, contentDescription = null, tint = KipuPrimary, modifier = Modifier.height(18.dp)) },
        title = text,
        body = null,
        compact = true,
    )
}

@Composable
private fun RowWithIcon(
    icon: @Composable () -> Unit,
    title: String,
    body: String?,
    compact: Boolean = false,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 0.dp),
        verticalAlignment = Alignment.Top,
    ) {
        icon()
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.Bold,
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
