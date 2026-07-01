package pe.kipu.app.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import pe.kipu.core.designsystem.component.KipuTestTags

val KipuBottomBarHeight = 72.dp
val KipuBottomBarBottomGap = 3.dp

/** Bottom navigation — HTML `.bottom-nav`. */
@Composable
fun KipuBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = KipuBottomBarBottomGap),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface.copy(alpha = 0.98f),
        contentColor = colors.onSurfaceVariant,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.8f)),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(KipuBottomBarHeight),
            containerColor = Color.Transparent,
            contentColor = colors.onSurfaceVariant,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            KipuDestination.bottomBarDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (selected) return@NavigationBarItem
                        if (destination == KipuDestination.Home) {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    modifier = Modifier.testTag(KipuTestTags.bottomBarTab(destination.route)),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        indicatorColor = colors.primary.copy(alpha = 0.1f),
                    ),
                )
            }
        }
    }
}
