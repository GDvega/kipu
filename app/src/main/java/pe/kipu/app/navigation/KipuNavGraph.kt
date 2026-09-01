package pe.kipu.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import pe.kipu.feature.juntas.GatheringsScreen
import pe.kipu.feature.juntas.navigation.GatheringRoutes
import pe.kipu.feature.movements.MovementsScreen
import pe.kipu.feature.plan.PlanWizardScreen
import pe.kipu.feature.profile.ProfileScreen
import pe.kipu.feature.profile.PrivacyPolicyScreen
import pe.kipu.feature.profile.navigation.ProfileRoutes
import pe.kipu.feature.receipts.ReceiptReviewScreen
import pe.kipu.feature.receipts.ReceiptsScreen
import pe.kipu.feature.receipts.navigation.ReceiptRoutes

@Composable
fun KipuNavGraph(
    navController: NavHostController,
    openManualMovementOnMovements: Boolean = false,
    onManualMovementLaunchConsumed: () -> Unit = {},
    onRequestManualMovement: () -> Unit = {},
    onCancelNewPlan: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = KipuDestination.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) },
    ) {
        composable(KipuDestination.Home.route) {
            HomeScreen(
                speedDialModalBottomPadding = KipuBottomBarHeight + KipuBottomBarBottomGap,
                onRegisterReceipt = {
                    navController.navigate(ReceiptRoutes.HUB)
                },
                onRegisterCash = onRequestManualMovement,
                onNavigateToMovements = {
                    navController.navigate(KipuDestination.Movements.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToCategoryMovements = { categoryId ->
                    navController.navigate(KipuPlanRoutes.movementsByCategory(categoryId))
                },
                onNavigateToPlan = { startStep ->
                    navController.navigate(KipuPlanRoutes.wizard(startStep))
                },
            )
        }
        composable(KipuDestination.Movements.route) {
            MovementsScreen(
                onRegisterReceipt = {
                    navController.navigate(ReceiptRoutes.HUB)
                },
                openManualOnLaunch = openManualMovementOnMovements,
                onOpenManualLaunchConsumed = onManualMovementLaunchConsumed,
            )
        }
        composable(
            route = KipuPlanRoutes.MOVEMENTS_BY_CATEGORY,
            arguments = listOf(
                navArgument(KipuPlanRoutes.CATEGORY_ID_ARG) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString(KipuPlanRoutes.CATEGORY_ID_ARG)
            MovementsScreen(
                initialCategoryId = categoryId,
                onRegisterReceipt = {
                    navController.navigate(ReceiptRoutes.HUB)
                },
            )
        }
        composable(KipuDestination.Envelopes.route) {
            EnvelopesScreen(
                onNavigateToMovements = { categoryId ->
                    navController.navigate(KipuPlanRoutes.movementsByCategory(categoryId))
                },
                onNavigateToPlan = { startStep ->
                    navController.navigate(KipuPlanRoutes.wizard(startStep))
                },
            )
        }
        composable(KipuDestination.Commitments.route) {
            CommitmentsScreen()
        }
        composable(KipuDestination.Profile.route) {
            ProfileScreen(
                onNavigateToPrivacyPolicy = {
                    navController.navigate(ProfileRoutes.PRIVACY)
                },
                onNavigateToSharedAccounts = {
                    navController.navigate(GatheringRoutes.LIST)
                },
            )
        }
        composable(ProfileRoutes.PRIVACY) {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(GatheringRoutes.LIST) {
            GatheringsScreen()
        }
        composable(
            route = KipuPlanRoutes.WIZARD,
            arguments = listOf(
                navArgument(KipuPlanRoutes.START_STEP_ARG) { type = NavType.StringType },
            ),
        ) {
            PlanWizardScreen(
                onFinished = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                onCancelNewPlan = onCancelNewPlan,
            )
        }

        composable(ReceiptRoutes.HUB) {
            ReceiptsScreen(
                onReviewReceipt = { contentUri ->
                    navController.navigate(ReceiptRoutes.review(contentUri))
                },
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
            )
        }
    }
}
