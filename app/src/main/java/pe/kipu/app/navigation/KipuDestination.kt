package pe.kipu.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class KipuDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : KipuDestination("home", "Inicio", Icons.Filled.Home)

    data object Movements : KipuDestination("movements", "Movimientos", Icons.AutoMirrored.Filled.List)

    data object Envelopes : KipuDestination("envelopes", "Sobres", Icons.Filled.Star)

    data object Commitments : KipuDestination("commitments", "Compromisos", Icons.Filled.Check)

    data object Profile : KipuDestination("profile", "Perfil", Icons.Filled.Person)

    companion object {
        val bottomBarDestinations = listOf(Home, Movements, Envelopes, Commitments, Profile)
    }
}
