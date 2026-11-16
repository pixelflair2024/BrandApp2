@file:OptIn(ExperimentalMaterial3Api::class)

package de.unseralltag.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.unseralltag.ui.screens.CalendarScreen
import de.unseralltag.ui.screens.ContractsScreen
import de.unseralltag.ui.screens.MealPlanScreen
import de.unseralltag.ui.screens.MoreScreen
import de.unseralltag.ui.screens.NotesScreen
import de.unseralltag.ui.screens.SettingsScreen
import de.unseralltag.ui.screens.ShoppingScreen
import de.unseralltag.ui.screens.SpecialDaysScreen
import de.unseralltag.ui.screens.TasksScreen
import de.unseralltag.ui.screens.TodayScreen
import de.unseralltag.ui.screens.WishlistScreen

object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val SHOPPING = "shopping"
    const val TASKS = "tasks"
    const val MORE = "more"
    const val SPECIAL_DAYS = "specialDays"
    const val WISHLIST = "wishlist"
    const val NOTES = "notes"
    const val MEALS = "meals"
    const val CONTRACTS = "contracts"
    const val SETTINGS = "settings"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, "Heute", Icons.Default.Home),
    Tab(Routes.CALENDAR, "Kalender", Icons.Default.CalendarMonth),
    Tab(Routes.SHOPPING, "Einkauf", Icons.Default.ShoppingCart),
    Tab(Routes.TASKS, "Aufgaben", Icons.Default.CheckCircle),
    Tab(Routes.MORE, "Mehr", Icons.Default.MoreHoriz),
)

/** Navigation, die in jedem Screen verfügbar ist. */
class Nav(private val nav: NavHostController) {
    fun go(route: String) {
        if (tabs.any { it.route == route }) {
            nav.navigate(route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            nav.navigate(route) { launchSingleTop = true }
        }
    }
    fun back() { nav.popBackStack() }
}

@Composable
fun MainScaffold() {
    val nav = rememberNavController()
    val n = Nav(nav)
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { n.go(t.route) },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = Routes.TODAY, modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable(Routes.TODAY) { TodayScreen(n) }
            composable(Routes.CALENDAR) { CalendarScreen(n) }
            composable(Routes.SHOPPING) { ShoppingScreen(n) }
            composable(Routes.TASKS) { TasksScreen(n) }
            composable(Routes.MORE) { MoreScreen(n) }
            composable(Routes.SPECIAL_DAYS) { SpecialDaysScreen(n) }
            composable(Routes.WISHLIST) { WishlistScreen(n) }
            composable(Routes.NOTES) { NotesScreen(n) }
            composable(Routes.MEALS) { MealPlanScreen(n) }
            composable(Routes.CONTRACTS) { ContractsScreen(n) }
            composable(Routes.SETTINGS) { SettingsScreen(n) }
        }
    }
}

/** Standard-Gerüst für einen Screen. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = {
            if (onAdd != null) FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Neu") }
        },
        content = content,
    )
}

