package pe.kipu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuAmberDim
import pe.kipu.core.designsystem.theme.KipuBlue
import pe.kipu.core.designsystem.theme.KipuBlueDim
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuPrimaryDim
import pe.kipu.core.designsystem.theme.KipuPurple
import pe.kipu.core.designsystem.theme.KipuPurpleDim
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.designsystem.theme.KipuRedDim

enum class KipuBadgeTone {
    Primary,
    Warning,
    Critical,
    Info,
    Purple,
}

/** Pill status chip — HTML `.daily-status` / `.mov-status`. */
@Composable
fun KipuBadge(
    text: String,
    tone: KipuBadgeTone,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = when (tone) {
        KipuBadgeTone.Primary -> KipuPrimaryDim to KipuPrimary
        KipuBadgeTone.Warning -> KipuAmberDim to KipuAmber
        KipuBadgeTone.Critical -> KipuRedDim to KipuRed
        KipuBadgeTone.Info -> KipuBlueDim to KipuBlue
        KipuBadgeTone.Purple -> KipuPurpleDim to KipuPurple
    }

    Text(
        text = text,
        modifier = modifier
            .background(color = background, shape = MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = foreground,
    )
}

/** Small tag — HTML `.mov-source` / `.source-tag`. */
@Composable
fun KipuCompactBadge(
    text: String,
    tone: KipuBadgeTone,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = when (tone) {
        KipuBadgeTone.Primary -> KipuPrimaryDim to KipuPrimary
        KipuBadgeTone.Warning -> KipuAmberDim to KipuAmber
        KipuBadgeTone.Critical -> KipuRedDim to KipuRed
        KipuBadgeTone.Info -> KipuBlueDim to KipuBlue
        KipuBadgeTone.Purple -> KipuPurpleDim to KipuPurple
    }

    Text(
        text = text.uppercase(),
        modifier = modifier
            .background(color = background, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = foreground,
    )
}
