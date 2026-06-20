package pe.kipu.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pe.kipu.feature.commitments.CommitmentsScreen
import pe.kipu.feature.envelopes.EnvelopesScreen
import pe.kipu.feature.home.HomeScreen
import pe.kipu.feature.movements.MovementsScreen
import pe.kipu.feature.plan.PlanWizardScreen
import pe.kipu.feature.profile.ProfileScreen
import pe.kipu.feature.juntas.GatheringsScreen
import pe.kipu.feature.juntas.navigation.GatheringRoutes
import pe.kipu.feature.receipts.ReceiptReviewScreen
import pe.kipu.feature.receipts.ReceiptsScreen
import pe.kipu.feature.receipts.navigation.ReceiptRoutes

@Composable
fun KipuNavGraph(
    navController: NavHostController,
    openManualMovementOnMovements: Boolean = false,
    onManualMovementLaunchConsumed: () -> Unit = {},
    onRequestManualMovement: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = KipuDestination.Home.route,
        modifier = modifier,
    ) {
        composable(KipuDestination.Home.route) {
            HomeScreen(
                onRegisterReceipt = {
                    navController.navigate(ReceiptRoutes.HUB)
                },
                onRegisterCash = onRequestManualMovement,
            )
        }
        composable(KipuDestination.Movements.route) {
            MovementsScreen(
                onRegisterReceipt = {
                    navController.navigate(ReceiptRoutes.HUB)
                },
                openManualOnLaunch = openManualMovementOnMovements,
            )
            if (openManualMovementOnMovements) {
                onManualMovementLaunchConsumed()
            }
        }
        composable(
            route = KipuPlanRoutes.MOVEMENTS_BY_CATEGORY,
            arguments = listOf(
                navArgument(KipuPlanRoutes.CATEGORY_ID_ARG) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString(KipuPlanRoutes.CATEGORY_ID_ARG)
            MovementsScreen(initialCategoryId = categoryId)
        }
        composable(KipuDestination.Envelopes.route) {
            EnvelopesScreen(
                onNavigateToMovements = { categoryId ->
                    navController.navigate(KipuPlanRoutes.movementsByCategory(categoryId))
                },
                onNavigateToPlan = { startStep ->
                    navController.navigate(KipuPlanRoutes.wizard(startStep))
                },
                onNavigateToCommitments = {
                    navController.navigate(KipuDestination.Commitments.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(KipuDestination.Commitments.route) {
            CommitmentsScreen()
        }
        composable(KipuDestination.Profile.route) {
            ProfileScreen(
                onNavigateToGatherings = {
                    navController.navigate(GatheringRoutes.LIST)
                },
            )
        }
        composable(
            route = KipuPlanRoutes.WIZARD,
            arguments = listOf(
                navArgument(KipuPlanRoutes.START_STEP_ARG) { type = NavType.StringType },
            ),
        ) {
            PlanWizardScreen(
                onFinished = { navController.popBackStack() },
            )
        }
        composable(GatheringRoutes.LIST) {
            GatheringsScreen()
        }
        composable(ReceiptRoutes.HUB) {
            ReceiptsScreen(
                onReviewReceipt = { contentUri ->
                    navController.navigate(ReceiptRoutes.review(contentUri))
                },
            )
        }
        composable(
            route = ReceiptRoutes.REVIEW,
            arguments = listOf(
                navArgument(ReceiptRoutes.CONTENT_URI_ARG) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            ReceiptReviewScreen(
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
