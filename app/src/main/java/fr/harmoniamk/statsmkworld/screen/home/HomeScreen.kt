package fr.harmoniamk.statsmkworld.screen.home

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.screen.registry.RegistryScreen
import fr.harmoniamk.statsmkworld.screen.stats.StatsScreen
import fr.harmoniamk.statsmkworld.screen.welcome.WelcomeScreen
import fr.harmoniamk.statsmkworld.ui.Colors

enum class BottomNavItem(var icon: Int, var route: String) {
    WELCOME(R.drawable.arrivee, "Home/Welcome"),
    STATS(R.drawable.stats, "Home/Stats"),
    REGISTRY(R.drawable.encyclopedie, "Home/Registry"),
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(onBack: () -> Unit, onTeamProfile: (String) -> Unit, onPlayerProfile: (String) -> Unit, onAddWar: () -> Unit, onCurrentWar: () -> Unit, onWarDetailsClick: (WarDetails) -> Unit) {
    val navController = rememberNavController()

    BackHandler { onBack() }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Colors.black) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                BottomNavItem.entries.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Colors.black,
                            unselectedIconColor = Colors.white,
                            indicatorColor = Colors.white
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },

        content = {
            NavHost(navController = navController, startDestination = "Home/Welcome") {
                composable(route = "Home/Welcome") {
                   WelcomeScreen(onTeamProfile = { onTeamProfile("me") }, onPlayerProfile = { onPlayerProfile("me") }, onAddWar = onAddWar, onCurrentWar = onCurrentWar, onWarDetailsClick = onWarDetailsClick)
                }
                composable(route = "Home/Stats") {
                    StatsScreen()
                }
                composable(route = "Home/Registry") {
                    RegistryScreen(onPlayerProfile = onPlayerProfile, onTeamProfile = onTeamProfile)
                }
            }
        }
    )
}
