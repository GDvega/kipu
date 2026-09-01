package pe.kipu.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class KipuDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : KipuDestination("home", "Inicio", Icons.Filled.Home)

    data object Movements : KipuDestination("movements", "Movimientos", Icons.Filled.ReceiptLong)

    data object Envelopes : KipuDestination("envelopes", "Sobres", Icons.Filled.AccountBalanceWallet)

    data object Commitments : KipuDestination("commitments", "Compromisos", Icons.Filled.Savings)

    data object Profile : KipuDestination("profile", "Perfil", Icons.Filled.AccountCircle)

    companion object {
        val bottomBarDestinations = listOf(Home, Movements, Envelopes, Commitments, Profile)
    }
}
