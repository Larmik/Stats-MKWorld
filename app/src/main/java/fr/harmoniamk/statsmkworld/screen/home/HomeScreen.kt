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
import fr.harmoniamk.statsmkworld.screen.profile.ProfileScreen
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullScreen
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullViewModel
import fr.harmoniamk.statsmkworld.screen.stats.ranking.StatsRankingScreen
import fr.harmoniamk.statsmkworld.screen.warList.WarListScreen
import fr.harmoniamk.statsmkworld.screen.warList.WarListViewModel
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
    onPlayerProfile: (String) -> Unit,
    onAddWar: (Boolean) -> Unit,
    onCurrentWar: () -> Unit,
    onWarDetailsClick: (WarDetails) -> Unit,
    // Ouvre « Voir par période » (#80) depuis le pôle Wars → graphe racine.
    onPeriodView: () -> Unit,
    onStats: (StatsType) -> Unit,
    onSearch: () -> Unit,
    // « Résultats → » du pôle Stats : historique filtré sur « me » sur le graphe racine (#65).
    onResults: () -> Unit,
    // « Classement entier » Circuits/Adversaires du pôle Stats → classement scopé (#67 round 3).
    // `isTeam` = portée courante (Équipe vs Individuel).
    onMapsRanking: (isTeam: Boolean) -> Unit,
    onOpponentsRanking: (isTeam: Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onDebug: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hors Accueil, ← ramène à l'Accueil (racine) ; depuis l'Accueil, ← quitte (rule 14).
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
                    // Pôle Wars : historique complet de l'équipe (userId = null).
                    WarListScreen(
                        viewModel = hiltViewModel(
                            key = "warlist-all",
                            creationCallback = { factory: WarListViewModel.Factory ->
                                factory.create(userId = null)
                            }
                        ),
                        onWarDetailsClick = onWarDetailsClick,
                        onAddWar = onAddWar,
                        onPeriodView = onPeriodView
                    )
                }
                composable(route = "Home/Stats") {
                    // Pôle Stats (#25) : onglets Individuelles/Équipe du joueur courant
                    // (userId = null → le VM le résout).
                    StatsFullScreen(
                        viewModel = hiltViewModel(
                            key = "me-stats",
                            creationCallback = { factory: StatsFullViewModel.Factory ->
                                factory.create(userId = null, showTabs = true)
                            }
                        ),
                        onResults = onResults,
                        onMapsSeeAll = onMapsRanking,
                        onOpponentsSeeAll = onOpponentsRanking
                    )
                }
                composable(route = "Home/Rankings") {
                    // Pôle Classements (#26) : sous-onglets Joueurs/Adversaires/Circuits ; les
                    // lignes mènent aux fiches via onStats.
                    StatsRankingScreen(
                        viewModel = hiltViewModel(key = "rankings"),
                        onStats = onStats
                    )
                }
                composable(route = "Home/Profile") {
                    // Pôle Profil (#28) : onglets fusionnés Joueur/Équipe.
                    ProfileScreen(
                        onBack = backToWelcome,
                        onPlayerClick = onPlayerProfile,
                        onDisconnect = onDisconnect,
                        onDebug = onDebug
                    )
                }
            }
        }
    )
}
