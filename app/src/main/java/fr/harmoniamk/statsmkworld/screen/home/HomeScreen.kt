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
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileScreen
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileViewModel
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullScreen
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullViewModel
import fr.harmoniamk.statsmkworld.screen.stats.menu.StatsMenuMode
import fr.harmoniamk.statsmkworld.screen.stats.menu.StatsMenuScreen
import fr.harmoniamk.statsmkworld.screen.warList.WarListScreen
import fr.harmoniamk.statsmkworld.screen.welcome.WelcomeScreen
import fr.harmoniamk.statsmkworld.ui.Colors

enum class BottomNavItem(var icon: Int, var route: String, val label: String) {
    WELCOME(R.drawable.ic_home, "Home/Welcome", "Accueil"),
    WARS(R.drawable.ic_wars, "Home/WarList", "Wars"),
    STATS(R.drawable.stats, "Home/Stats", "Statistiques"),
    RANKINGS(R.drawable.ic_podium, "Home/Rankings", "Classements"),
    PROFILE(R.drawable.ic_profile, "Home/Profile", "Profil"),
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    onBack: () -> Unit,
    onTeamProfile: (String) -> Unit,
    onAddWar: (Boolean) -> Unit,
    onCurrentWar: () -> Unit,
    // onAddWar est désormais consommé par le pôle Wars (WarListScreen), plus par
    // l'Accueil : le sélecteur/CTA « Nouvelle war » a déménagé vers le pôle Wars.
    onWarDetailsClick: (WarDetails) -> Unit,
    onStats: (StatsType) -> Unit,
    onRanking: (StatsType?) -> Unit,
    onSearch: () -> Unit,
    onDisconnect: () -> Unit,
    onDebug: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Depuis n'importe quel pôle autre qu'Accueil, ← ramène au pôle Accueil (racine du
    // NavHost imbriqué) ; depuis Accueil, ← quitte l'app (onBack).
    val onWelcome = currentDestination?.hierarchy?.any { it.route == BottomNavItem.WELCOME.route } == true
    val backToWelcome: () -> Unit = {
        navController.navigate(BottomNavItem.WELCOME.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    BackHandler {
        when (onWelcome) {
            true -> onBack()
            else -> backToWelcome()
        }
    }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Colors.black) {
                BottomNavItem.entries.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = screen.label,
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
                    WelcomeScreen(
                        onTeamProfile = { onTeamProfile("me") },
                        onCurrentWar = onCurrentWar,
                        onWarDetailsClick = onWarDetailsClick,
                        onWarListClick = { navController.navigate("Home/WarList") },
                        onSearch = onSearch
                    )
                }
                composable(route = "Home/WarList") {
                    WarListScreen(
                        onWarDetailsClick = onWarDetailsClick,
                        onAddWar = onAddWar,
                        onCurrentWar = onCurrentWar
                    )
                }
                composable(route = "Home/Stats") {
                    // Pôle Stats (ticket #25) : écran riche à onglets Individuelles /
                    // Équipe pour le joueur courant (« mes stats »). userId = null →
                    // le VM résout le joueur courant.
                    StatsFullScreen(
                        viewModel = hiltViewModel(
                            key = "me-stats",
                            creationCallback = { factory: StatsFullViewModel.Factory ->
                                factory.create(userId = null, showTabs = true)
                            }
                        ),
                        onResults = { navController.navigate("Home/WarList") }
                    )
                }
                composable(route = "Home/Rankings") {
                    StatsMenuScreen(
                        mode = StatsMenuMode.RANKINGS,
                        onClick = onStats,
                        onRanking = onRanking,
                        onSearch = onSearch
                    )
                }
                composable(route = "Home/Profile") {
                    PlayerProfileScreen(
                        viewModel = hiltViewModel(
                            key = "me",
                            creationCallback = { factory: PlayerProfileViewModel.Factory ->
                                factory.create("me")
                            }
                        ),
                        onBack = backToWelcome,
                        onDisconnect = onDisconnect,
                        onDebug = onDebug
                    )
                }
            }
        }
    )
}
