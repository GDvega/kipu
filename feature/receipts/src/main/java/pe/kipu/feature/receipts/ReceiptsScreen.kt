package pe.kipu.feature.receipts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuLayout
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSubScreenScaffold

@Composable
fun ReceiptsScreen(
    onReviewReceipt: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { onReviewReceipt(it.toString()) }
    }

    KipuSubScreenScaffold(
        title = "Comprobantes",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = KipuLayout.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(KipuLayout.sectionSpacing),
        ) {
            Text(
                text = "Registra pagos Yape o Plin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KipuCard {
                Text(
                    text = "Comparte el comprobante desde Yape o Plin y elige Kipu, o selecciona una imagen guardada.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            KipuPrimaryButton(
                text = "Elegir imagen",
                onClick = {
                    pickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
