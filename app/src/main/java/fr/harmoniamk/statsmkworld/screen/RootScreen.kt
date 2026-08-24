package fr.harmoniamk.statsmkworld.screen

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.screen.addTrack.AddTrackScreen
import fr.harmoniamk.statsmkworld.screen.addTrack.AddTrackViewModel
import fr.harmoniamk.statsmkworld.screen.addWar.AddWarScreen
import fr.harmoniamk.statsmkworld.screen.addWar.AddWarViewModel
import fr.harmoniamk.statsmkworld.screen.currentWar.CurrentWarActionsScreen
import fr.harmoniamk.statsmkworld.screen.currentWar.CurrentWarScreen
import fr.harmoniamk.statsmkworld.screen.debug.DebugScreen
import fr.harmoniamk.statsmkworld.screen.editTab.EditTabScreen
import fr.harmoniamk.statsmkworld.screen.editTab.EditTabViewModel
import fr.harmoniamk.statsmkworld.screen.editTrack.EditTrackScreen
import fr.harmoniamk.statsmkworld.screen.editTrack.EditTrackViewModel
import fr.harmoniamk.statsmkworld.screen.home.HomeScreen
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileScreen
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileViewModel
import fr.harmoniamk.statsmkworld.screen.registry.RegistryScreen
import fr.harmoniamk.statsmkworld.screen.signup.SignupScreen
import fr.harmoniamk.statsmkworld.screen.signup.SignupViewModel
import fr.harmoniamk.statsmkworld.screen.stats.StatsScreen
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullScreen
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullViewModel
import fr.harmoniamk.statsmkworld.screen.stats.map.MapDetailScreen
import fr.harmoniamk.statsmkworld.screen.stats.map.MapDetailViewModel
import fr.harmoniamk.statsmkworld.screen.stats.map.MapPilotsRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentDetailScreen
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentDetailViewModel
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentTracksRankingScreen
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileScreen
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileViewModel
import fr.harmoniamk.statsmkworld.screen.trackDetails.TrackDetailsScreen
import fr.harmoniamk.statsmkworld.screen.trackDetails.TrackDetailsViewModel
import fr.harmoniamk.statsmkworld.screen.warDetails.WarDetailsScreen
import fr.harmoniamk.statsmkworld.screen.warDetails.WarDetailsViewModel
import fr.harmoniamk.statsmkworld.screen.warList.WarListScreen
import fr.harmoniamk.statsmkworld.screen.warList.WarListViewModel
import fr.harmoniamk.statsmkworld.worker.MKWorkerBuilder
import fr.harmoniamk.statsmkworld.worker.UpdateDataWorker

@Composable
fun RootScreen(startDestination: String, code: String = "", onBack: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        MKWorkerBuilder.enqueueUniquePeriodicWork<UpdateDataWorker>(context = context)
    }
    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(700)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(700)) },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(700)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(700)
            )
        }

    ) {

        composable(route = "Signup") {
            SignupScreen(
                viewModel = hiltViewModel(
                    key = code + System.currentTimeMillis().toString(),
                    creationCallback = { factory: SignupViewModel.Factory ->
                        factory.create(code)
                    }
                ),
                onBack = onBack,
                onNext = { navController.navigate("Home") }
            )
        }

        composable(route = "Home") {
            HomeScreen(
                onBack = onBack,
                onTeamProfile = { navController.navigate("Team/Profile/$it") },
                onPlayerProfile = { navController.navigate("Player/Profile/$it") },
                onAddWar = { navController.navigate("Home/AddWar/$it") },
                onCurrentWar = { navController.navigate("Home/CurrentWar") },
                onWarDetailsClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", it)
                    navController.navigate("Home/WarDetails")
                },
                onStats = { type ->
                    // Fiches dédiées Joueur (#65 : StatsFullScreen centré sur le joueur cliqué,
                    // sans sélecteur Indiv/Équipe), Adversaire (#27) et Circuit (#27) ; les autres
                    // portées (ex. équipe) restent sur l'écran Stats générique. Le userId (nullable)
                    // sème le mode initial Indiv/Équipe de la fiche (rule 11) ; « null » = Équipe.
                    when (type) {
                        is StatsType.PlayerStats -> navController.navigate("Statsfull/${type.userId}")
                        is StatsType.OpponentStats -> navController.navigate("Opponent/${type.teamId}/${type.userId ?: "null"}")
                        is StatsType.MapStats -> navController.navigate("Map/${type.trackIndex?.joinToString(",").orEmpty()}/${type.userId ?: "null"}")
                        else -> {
                            navController.currentBackStackEntry?.savedStateHandle?.set("type", type)
                            navController.navigate("Stats")
                        }
                    }
                },
                onSearch = { navController.navigate("Home/Registry") },
                // Lien « Résultats → » du pôle Stats (joueur courant) : historique filtré
                // sur « me », poussé sur le GRAPHE RACINE (route accessible depuis ici),
                // évitant la route interne au NavHost de HomeScreen (#65).
                onResults = { navController.navigate("Home/WarList/me") },
                onDisconnect = { navController.navigate("Signup") },
                onDebug = { navController.navigate("Player/Profile/Debug") }
            )
        }

        composable(route = "Home/Registry") {
            RegistryScreen(
                onBack = { navController.popBackStack() },
                onPlayerProfile = { navController.navigate("Player/Profile/$it") },
                onTeamProfile = { navController.navigate("Team/Profile/$it") }
            )
        }

        composable("Stats") {
            val type =
                navController.previousBackStackEntry?.savedStateHandle?.get<StatsType>("type")
            StatsScreen(
                viewModel = hiltViewModel(
                    creationCallback = { factory: fr.harmoniamk.statsmkworld.screen.stats.StatsViewModel.Factory ->
                        factory.create(type)
                    }
                ),
                onBack = { navController.popBackStack() },
                onWarDetailsClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", it)
                    navController.navigate("Home/WarDetails")
                }
            )
        }

        // Fiche détail ADVERSAIRE (#27) : atteinte depuis les Classements/Résultats
        // (et, à terme, la fiche équipe publique #28). teamId = identifiant d'opposant ;
        // userId (« null » = Équipe) sème le mode initial Indiv/Équipe.
        composable(
            route = "Opponent/{teamId}/{userId}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true }
            )
        ) {
            val teamId = it.arguments?.getString("teamId").orEmpty()
            val userId = it.arguments?.getString("userId")
            OpponentDetailScreen(
                viewModel = hiltViewModel(
                    key = "$teamId-$userId",
                    creationCallback = { factory: OpponentDetailViewModel.Factory ->
                        factory.create(teamId = teamId, initialUserId = userId)
                    }
                ),
                onBack = { navController.popBackStack() },
                onWarDetailsClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", it)
                    navController.navigate("Home/WarDetails")
                },
                onTracksRanking = { navController.navigate("Opponent/$teamId/$userId/Tracks") }
            )
        }

        // Classement complet des circuits joués contre l'adversaire (« Voir en entier »).
        composable(
            route = "Opponent/{teamId}/{userId}/Tracks",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true }
            )
        ) {
            val teamId = it.arguments?.getString("teamId").orEmpty()
            val userId = it.arguments?.getString("userId")
            OpponentTracksRankingScreen(
                viewModel = hiltViewModel(
                    key = "$teamId-$userId-tracks",
                    creationCallback = { factory: OpponentDetailViewModel.Factory ->
                        factory.create(teamId = teamId, initialUserId = userId)
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Fiche détail CIRCUIT (#27) : atteinte depuis les Classements. trackIndex =
        // index(es) de map (CSV) ; userId (« null » = Équipe) sème le mode initial.
        composable(
            route = "Map/{trackIndex}/{userId}",
            arguments = listOf(
                navArgument("trackIndex") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true }
            )
        ) {
            val trackIndex = it.arguments?.getString("trackIndex")
                ?.split(",")
                ?.mapNotNull { part -> part.toIntOrNull() }
                .orEmpty()
            val userId = it.arguments?.getString("userId")
            val csv = trackIndex.joinToString(",")
            MapDetailScreen(
                viewModel = hiltViewModel(
                    key = "$csv-$userId",
                    creationCallback = { factory: MapDetailViewModel.Factory ->
                        factory.create(trackIndex = trackIndex, initialUserId = userId)
                    }
                ),
                onBack = { navController.popBackStack() },
                onPilotsRanking = { navController.navigate("Map/$csv/$userId/Pilots") }
            )
        }

        // Classement complet des pilotes sur le circuit (« Voir en entier »).
        composable(
            route = "Map/{trackIndex}/{userId}/Pilots",
            arguments = listOf(
                navArgument("trackIndex") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true }
            )
        ) {
            val trackIndex = it.arguments?.getString("trackIndex")
                ?.split(",")
                ?.mapNotNull { part -> part.toIntOrNull() }
                .orEmpty()
            val userId = it.arguments?.getString("userId")
            MapPilotsRankingScreen(
                viewModel = hiltViewModel(
                    key = "${trackIndex.joinToString(",")}-$userId-pilots",
                    creationCallback = { factory: MapDetailViewModel.Factory ->
                        factory.create(trackIndex = trackIndex, initialUserId = userId)
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // statsfull (ticket #25) : stats détaillées d'un joueur donné (variante
        // « pour un joueur donné » de la vue Individuelles, mutualisée). Atteinte
        // depuis les Classements (#26) et la fiche joueur (Profil) — points d'entrée
        // relevant d'autres tickets ; ici la route réutilisable est en place.
        composable(
            route = "Statsfull/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId")
            StatsFullScreen(
                viewModel = hiltViewModel(
                    key = userId,
                    creationCallback = { factory: StatsFullViewModel.Factory ->
                        factory.create(userId = userId, showTabs = false)
                    }
                ),
                onBack = { navController.popBackStack() },
                // « Résultats → » : historique filtré sur CE joueur (#65).
                onResults = { navController.navigate("Home/WarList/$userId") }
            )
        }

        // Historique des wars FILTRÉ sur un joueur (#65), poussé sur le graphe racine
        // (au-dessus du pôle) → back = retour à StatsFullScreen (rule 14). `userId` = id
        // du joueur, ou « me » = joueur courant (résolu par le VM).
        composable(
            route = "Home/WarList/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId")
            WarListScreen(
                viewModel = hiltViewModel(
                    key = "warlist-$userId",
                    creationCallback = { factory: WarListViewModel.Factory ->
                        factory.create(userId = userId)
                    }
                ),
                onWarDetailsClick = { warDetails ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", warDetails)
                    navController.navigate("Home/WarDetails")
                },
                onAddWar = { navController.navigate("Home/AddWar/$it") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "Player/Profile/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val id = it.arguments?.getString("id")
            PlayerProfileScreen(
                viewModel = hiltViewModel(
                    key = id.toString(),
                    creationCallback = { factory: PlayerProfileViewModel.Factory ->
                        factory.create(id.toString())
                    }
                ),
                onBack = { navController.popBackStack() },
                onDisconnect = { navController.navigate("Signup") },
                onDebug = { navController.navigate("Player/Profile/Debug") }
            )
        }

        composable(
            route = "Team/Profile/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val id = it.arguments?.getString("id")
            TeamProfileScreen(
                viewModel = hiltViewModel(
                    key = id.toString(),
                    creationCallback = { factory: TeamProfileViewModel.Factory ->
                        factory.create(id.toString())
                    }),
                onBack = { navController.popBackStack() },
                onPlayerClick = { navController.navigate("Player/Profile/$it") }
            )
        }

        composable(
            route = "Home/AddWar/{is24p}",
            arguments = listOf(navArgument("is24p") { type = NavType.BoolType })

        ) {
            // L'argument de route ne sert qu'à SEMER le mode initial du VM ; la
            // bascule 12/24 se fait ensuite en interne (état réactif, même écran,
            // sans re-navigation).
            val is24p = it.arguments?.getBoolean("is24p")
            AddWarScreen(
                viewModel = hiltViewModel(
                    creationCallback = { factory: AddWarViewModel.Factory -> factory.create(is24p = is24p == true) }
                ),
                onBack = {
                navController.popBackStack()
            }, onCurrentWar = {
                navController.popBackStack()
                navController.navigate(route = "Home/CurrentWar")
            })
        }

        composable(route = "Home/CurrentWar") {
            val backToHome: () -> Unit = {
                navController.navigate("Home") {
                    popUpTo("Home") { inclusive = true }
                    launchSingleTop = true
                }
            }
            CurrentWarScreen(
                onBack = backToHome,
                onAddTrack = { navController.navigate(route = "Home/CurrentWar/AddTrack/$it") },
                onActions = { navController.navigate("Home/CurrentWar/Actions") },
                onTrackDetails = { track, courseNumber ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("track", track)
                    navController.currentBackStackEntry?.savedStateHandle?.set("courseNumber", courseNumber)
                    navController.navigate("Home/TrackDetails/true")
                },
                onWarValidated = backToHome,
            )
        }

        composable(
            route = "Home/CurrentWar/AddTrack/{is24p}",
            arguments = listOf(navArgument("is24p") { type = NavType.BoolType })

        ) {
            val is24p = it.arguments?.getBoolean("is24p")

            AddTrackScreen(
                viewModel = hiltViewModel { factory: AddTrackViewModel.Factory ->
                    factory.create(is24p = is24p == true)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = "Home/CurrentWar/Actions") {
            CurrentWarActionsScreen(onBack = { navController.popBackStack() }, onBackToWelcome = {
                navController.navigate(route = "Home")
            })
        }

        composable(route = "Home/WarDetails") {
            val war = navController.previousBackStackEntry?.savedStateHandle?.get<WarDetails>("war")
            WarDetailsScreen(
                viewModel = hiltViewModel(
                    key = war?.war?.id.toString(),
                    creationCallback = { factory: WarDetailsViewModel.Factory ->
                        factory.create(war)
                    }
                ),
                onBack = { navController.popBackStack() },
                onTrackClick = { track, courseNumber ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("track", track)
                    navController.currentBackStackEntry?.savedStateHandle?.set("courseNumber", courseNumber)
                    navController.navigate("Home/TrackDetails/false")
                },
                onTab = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("details", it)
                    navController.navigate("Home/WarDetails/Tab")
                },
                // « Voir l'adversaire » → fiche adversaire (12 j : opposant unique). Le teamId
                // est l'id d'opposant de la war (rosterId, ou teamId legacy) ; userId « null »
                // = portée Équipe (rule 15, cf. autres appels d'Opponent).
                onOpponent = { opponentId ->
                    navController.navigate("Opponent/$opponentId/null")
                }
            )
        }

        composable("Home/WarDetails/Tab") {
            val details = navController.previousBackStackEntry?.savedStateHandle?.get<WarDetails>("details")
            EditTabScreen(
                viewModel = hiltViewModel(
                    key = details?.war?.id.toString(),
                    creationCallback = { factory: EditTabViewModel.Factory ->
                        factory.create(details)
                    }
                ), onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "Home/TrackDetails/{editing}",
            arguments = listOf(navArgument("editing") { type = NavType.BoolType })

        ) {
            val savedState = navController.previousBackStackEntry?.savedStateHandle
            val track = savedState?.get<WarTrackDetails>("track")
            val courseNumber = savedState?.get<Int>("courseNumber") ?: 0
            val editing = it.arguments?.getBoolean("editing") == true
            TrackDetailsScreen(
                viewModel = hiltViewModel(
                    key = track?.track?.id.toString(),
                    creationCallback = { factory: TrackDetailsViewModel.Factory ->
                        factory.create(track, editing, courseNumber)
                    }
                ),
                onBack = { navController.popBackStack() },
                onEditTrack = { details, is24p ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("track", details)
                    navController.navigate("Home/EditTrack/$is24p")
                }
            )
        }

        composable(
            route = "Home/EditTrack/{is24p}",
            arguments = listOf(navArgument("is24p") { type = NavType.BoolType })

        ) {
            val track = navController.previousBackStackEntry?.savedStateHandle?.get<WarTrackDetails>("track")
            val is24p = it.arguments?.getBoolean("is24p")
            EditTrackScreen(
                viewModel = hiltViewModel(
                    key = track?.track?.id.toString(),
                    creationCallback = { factory: EditTrackViewModel.Factory ->
                        factory.create(track, is24p == true)
                    }
                ),
                onBack = { navController.popBackStack() },
                onBackToCurrent = { navController.navigate("Home/CurrentWar") },
            )
        }
        composable("Player/Profile/Debug") {
            DebugScreen { navController.popBackStack() }
        }

    }
}