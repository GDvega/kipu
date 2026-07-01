package pe.kipu.app

import android.content.Intent
import pe.kipu.app.receipt.ReceiptShareIntentParser
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pe.kipu.app.navigation.KipuBottomBar
import pe.kipu.app.navigation.KipuDestination
import pe.kipu.app.navigation.KipuNavGraph
import pe.kipu.app.navigation.KipuPlanRoutes
import pe.kipu.app.presentation.MainViewModel
import pe.kipu.core.designsystem.component.KipuScreenBackground
import pe.kipu.core.designsystem.component.KipuSystemBarStyle
import pe.kipu.core.designsystem.theme.KipuTheme
import pe.kipu.core.domain.model.resolvesToDarkTheme
import pe.kipu.feature.onboarding.OnboardingScreen
import pe.kipu.feature.receipts.navigation.ReceiptRoutes

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var incomingShareUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingShareUri = ReceiptShareIntentParser.extractImageUri(intent)?.toString()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = themeMode.resolvesToDarkTheme(systemInDarkTheme)

            KipuTheme(darkTheme = darkTheme) {
                KipuSystemBarStyle(darkTheme = darkTheme)
                val onboardingCompleted by mainViewModel.onboardingCompleted.collectAsStateWithLifecycle()
                val pendingPlanWizard by mainViewModel.pendingPlanWizard.collectAsStateWithLifecycle()
                val pendingReceiptUri by mainViewModel.pendingReceiptUri.collectAsStateWithLifecycle()
                val shareUri = incomingShareUri

                LaunchedEffect(shareUri) {
                    shareUri?.let { uri ->
                        mainViewModel.onSharedReceiptUri(uri)
                        incomingShareUri = null
                    }
                }

                KipuScreenBackground {
                    if (!onboardingCompleted) {
                        OnboardingScreen()
                    } else {
                        val navController = rememberNavController()
                        var openManualOnMovements by remember { mutableStateOf(false) }
                        var planWizardHandled by remember { mutableStateOf(false) }
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val showBottomBar = KipuDestination.bottomBarDestinations.any { destination ->
                            destination.route == currentRoute
                        }

                        LaunchedEffect(onboardingCompleted, pendingPlanWizard, planWizardHandled) {
                            if (onboardingCompleted && pendingPlanWizard && !planWizardHandled) {
                                planWizardHandled = true
                                navController.navigate(KipuPlanRoutes.wizard())
                                mainViewModel.clearPendingPlanWizard()
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (mainViewModel.consumePendingOpenManualMovement()) {
                                openManualOnMovements = true
                                navController.navigate(KipuDestination.Movements.route)
                            }
                            mainViewModel.consumePendingReceiptUri()?.let { uri ->
                                navController.navigate(ReceiptRoutes.review(uri))
                            }
                        }

                        LaunchedEffect(pendingReceiptUri) {
                            pendingReceiptUri?.let { uri ->
                                mainViewModel.consumePendingReceiptUri()
                                navController.navigate(ReceiptRoutes.review(uri))
                            }
                        }

                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                                if (showBottomBar) {
                                    KipuBottomBar(navController = navController)
                                }
                            },
                        ) { innerPadding ->
                            KipuNavGraph(
                                navController = navController,
                                openManualMovementOnMovements = openManualOnMovements,
                                onManualMovementLaunchConsumed = { openManualOnMovements = false },
                                onRequestManualMovement = {
                                    openManualOnMovements = true
                                    navController.navigate(KipuDestination.Movements.route)
                                },
                                onCancelNewPlan = { mainViewModel.resetOnboarding() },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShareUri = ReceiptShareIntentParser.extractImageUri(intent)?.toString()
    }
}
