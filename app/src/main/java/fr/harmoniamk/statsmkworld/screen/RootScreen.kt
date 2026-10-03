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
import fr.harmoniamk.statsmkworld.model.local.WarKindFilter
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
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.full.PlayerMapsRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.full.PlayerOpponentsRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullScreen
import fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullViewModel
import fr.harmoniamk.statsmkworld.screen.stats.map.MapBaggersRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.map.MapDetailScreen
import fr.harmoniamk.statsmkworld.screen.stats.map.MapDetailViewModel
import fr.harmoniamk.statsmkworld.screen.stats.map.MapOpponentsRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.map.MapPilotsRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentBaggersRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentDetailScreen
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentDetailViewModel
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentPilotsRankingScreen
import fr.harmoniamk.statsmkworld.screen.stats.opponent.OpponentTracksRankingScreen
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileScreen
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileViewModel
import fr.harmoniamk.statsmkworld.screen.trackDetails.TrackDetailsScreen
import fr.harmoniamk.statsmkworld.screen.trackDetails.TrackDetailsViewModel
import fr.harmoniamk.statsmkworld.screen.warDetails.WarDetailsScreen
import fr.harmoniamk.statsmkworld.screen.warDetails.WarDetailsViewModel
import fr.harmoniamk.statsmkworld.screen.warList.WarListScreen
import fr.harmoniamk.statsmkworld.screen.warList.WarListViewModel
import fr.harmoniamk.statsmkworld.screen.warList.period.PeriodScreen
import fr.harmoniamk.statsmkworld.screen.warList.period.PeriodViewModel
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
                // « Voir par période » (#80) : écran de graphe racine par-dessus le pôle Wars.
                // Filtre Amicaux/Officiels (#103) propagé aux enfants en segment de route `{kind}`.
                onPeriodView = { kindFilter -> navController.navigate("Home/Period/${kindFilter.routeSegment}") },
                onStats = { type ->
                    // userId (nullable) sème le mode initial Indiv/Équipe (rule 11) ; « null » = Équipe.
                    when (type) {
                        is StatsType.PlayerStats -> navController.navigate("Statsfull/${type.userId}/${type.kindFilter.routeSegment}")
                        // Saison propagée (#91 pt.5) en segment de route : « all » = tout l'historique.
                        is StatsType.OpponentStats -> navController.navigate("Opponent/${type.teamId}/${type.userId ?: "null"}/${type.seasonNumber ?: "all"}/${type.kindFilter.routeSegment}")
                        is StatsType.MapStats -> navController.navigate("Map/${type.trackIndex?.joinToString(",").orEmpty()}/${type.userId ?: "null"}/${type.seasonNumber ?: "all"}/${type.kindFilter.routeSegment}")
                    }
                },
                onSearch = { navController.navigate("Home/Registry") },
                // « Résultats → » du pôle Stats : historique filtré sur « me », graphe racine (#65).
                onResults = { kindFilter -> navController.navigate("Home/WarList/me/${kindFilter.routeSegment}") },
                // « Classement entier » Circuits/Adversaires du pôle Stats : scopé « me »
                // (joueur courant), isTeam = portée Équipe, graphe racine (#67 round 3).
                onMapsRanking = { isTeam, kindFilter -> navController.navigate("Statsfull/me/${kindFilter.routeSegment}/Maps/$isTeam") },
                onOpponentsRanking = { isTeam, kindFilter -> navController.navigate("Statsfull/me/${kindFilter.routeSegment}/Opponents/$isTeam") },
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

        // Fiche détail ADVERSAIRE (#27). teamId = id d'opposant ; userId (« null » = Équipe)
        // sème le mode initial Indiv/Équipe.
        composable(
            route = "Opponent/{teamId}/{userId}/{season}/{kind}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val teamId = it.arguments?.getString("teamId").orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            OpponentDetailScreen(
                viewModel = hiltViewModel(
                    key = "$teamId-$userId-$season-$kind",
                    creationCallback = { factory: OpponentDetailViewModel.Factory ->
                        factory.create(teamId = teamId, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() },
                onWarDetailsClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", it)
                    navController.navigate("Home/WarDetails")
                },
                onTracksRanking = { navController.navigate("Opponent/$teamId/$userId/$season/$kind/Tracks") },
                onPilotsRanking = { navController.navigate("Opponent/$teamId/$userId/$season/$kind/Pilots") },
                onBaggersRanking = { navController.navigate("Opponent/$teamId/$userId/$season/$kind/Baggers") }
            )
        }

        // Classement complet des circuits joués contre l'adversaire (« Voir en entier »).
        composable(
            route = "Opponent/{teamId}/{userId}/{season}/{kind}/Tracks",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val teamId = it.arguments?.getString("teamId").orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            OpponentTracksRankingScreen(
                viewModel = hiltViewModel(
                    key = "$teamId-$userId-$season-$kind-tracks",
                    creationCallback = { factory: OpponentDetailViewModel.Factory ->
                        factory.create(teamId = teamId, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Classement complet des pilotes ayant joué contre l'adversaire (« Voir en entier » #67).
        composable(
            route = "Opponent/{teamId}/{userId}/{season}/{kind}/Pilots",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val teamId = it.arguments?.getString("teamId").orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            OpponentPilotsRankingScreen(
                viewModel = hiltViewModel(
                    key = "$teamId-$userId-$season-$kind-pilots",
                    creationCallback = { factory: OpponentDetailViewModel.Factory ->
                        factory.create(teamId = teamId, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Classement complet des baggeurs face à l'adversaire (« Voir en entier » #69).
        composable(
            route = "Opponent/{teamId}/{userId}/{season}/{kind}/Baggers",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val teamId = it.arguments?.getString("teamId").orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            OpponentBaggersRankingScreen(
                viewModel = hiltViewModel(
                    key = "$teamId-$userId-$season-$kind-baggers",
                    creationCallback = { factory: OpponentDetailViewModel.Factory ->
                        factory.create(teamId = teamId, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Fiche détail CIRCUIT (#27). trackIndex = index(es) de map (CSV) ;
        // userId (« null » = Équipe) sème le mode initial.
        composable(
            route = "Map/{trackIndex}/{userId}/{season}/{kind}",
            arguments = listOf(
                navArgument("trackIndex") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val trackIndex = it.arguments?.getString("trackIndex")
                ?.split(",")
                ?.mapNotNull { part -> part.toIntOrNull() }
                .orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            val csv = trackIndex.joinToString(",")
            MapDetailScreen(
                viewModel = hiltViewModel(
                    key = "$csv-$userId-$season-$kind",
                    creationCallback = { factory: MapDetailViewModel.Factory ->
                        factory.create(trackIndex = trackIndex, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() },
                onPilotsRanking = { navController.navigate("Map/$csv/$userId/$season/$kind/Pilots") },
                onBaggersRanking = { navController.navigate("Map/$csv/$userId/$season/$kind/Baggers") },
                onOpponentsRanking = { navController.navigate("Map/$csv/$userId/$season/$kind/Opponents") }
            )
        }

        // Classement complet des pilotes sur le circuit (« Voir en entier »).
        composable(
            route = "Map/{trackIndex}/{userId}/{season}/{kind}/Pilots",
            arguments = listOf(
                navArgument("trackIndex") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val trackIndex = it.arguments?.getString("trackIndex")
                ?.split(",")
                ?.mapNotNull { part -> part.toIntOrNull() }
                .orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            MapPilotsRankingScreen(
                viewModel = hiltViewModel(
                    key = "${trackIndex.joinToString(",")}-$userId-$season-$kind-pilots",
                    creationCallback = { factory: MapDetailViewModel.Factory ->
                        factory.create(trackIndex = trackIndex, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Classement complet des baggeurs sur le circuit (« Voir en entier » #69).
        composable(
            route = "Map/{trackIndex}/{userId}/{season}/{kind}/Baggers",
            arguments = listOf(
                navArgument("trackIndex") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val trackIndex = it.arguments?.getString("trackIndex")
                ?.split(",")
                ?.mapNotNull { part -> part.toIntOrNull() }
                .orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            MapBaggersRankingScreen(
                viewModel = hiltViewModel(
                    key = "${trackIndex.joinToString(",")}-$userId-$season-$kind-baggers",
                    creationCallback = { factory: MapDetailViewModel.Factory ->
                        factory.create(trackIndex = trackIndex, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Classement complet des adversaires rencontrés sur le circuit (« Voir en entier » #67).
        composable(
            route = "Map/{trackIndex}/{userId}/{season}/{kind}/Opponents",
            arguments = listOf(
                navArgument("trackIndex") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("season") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val trackIndex = it.arguments?.getString("trackIndex")
                ?.split(",")
                ?.mapNotNull { part -> part.toIntOrNull() }
                .orEmpty()
            val userId = it.arguments?.getString("userId")
            val season = it.arguments?.getString("season")
            val kind = it.arguments?.getString("kind")
            MapOpponentsRankingScreen(
                viewModel = hiltViewModel(
                    key = "${trackIndex.joinToString(",")}-$userId-$season-$kind-opponents",
                    creationCallback = { factory: MapDetailViewModel.Factory ->
                        factory.create(trackIndex = trackIndex, initialUserId = userId, seasonNumber = season.toSeasonNumber(), kindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Stats détaillées d'un joueur donné (variante « pour un joueur » des Individuelles, #25).
        composable(
            route = "Statsfull/{userId}/{kind}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val userId = it.arguments?.getString("userId")
            val kind = it.arguments?.getString("kind")
            StatsFullScreen(
                viewModel = hiltViewModel(
                    key = "$userId-$kind",
                    creationCallback = { factory: StatsFullViewModel.Factory ->
                        factory.create(userId = userId, showTabs = false, initialKindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onBack = { navController.popBackStack() },
                // « Résultats → » : historique filtré sur CE joueur (#65).
                onResults = { kindFilter -> navController.navigate("Home/WarList/$userId/${kindFilter.routeSegment}") },
                // « Classement entier » Circuits/Adversaires scopé à CE joueur (#67 round 3).
                // showTabs=false ⇒ toujours Individuelles ⇒ isTeam = false.
                onMapsSeeAll = { isTeam, kindFilter -> navController.navigate("Statsfull/$userId/${kindFilter.routeSegment}/Maps/$isTeam") },
                onOpponentsSeeAll = { isTeam, kindFilter -> navController.navigate("Statsfull/$userId/${kindFilter.routeSegment}/Opponents/$isTeam") }
            )
        }

        // Classement complet des CIRCUITS scopé à un joueur/équipe (#67 round 3).
        // userId « me » ⇒ joueur courant.
        composable(
            route = "Statsfull/{userId}/{kind}/Maps/{isTeam}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType },
                navArgument("isTeam") { type = NavType.BoolType }
            )
        ) {
            val userIdArg = it.arguments?.getString("userId")?.takeIf { id -> id != "me" }
            val kind = it.arguments?.getString("kind")
            val isTeam = it.arguments?.getBoolean("isTeam") == true
            PlayerMapsRankingScreen(
                viewModel = hiltViewModel(
                    key = "$userIdArg-maps-$isTeam-$kind",
                    creationCallback = { factory: StatsFullViewModel.Factory ->
                        factory.create(userId = userIdArg, showTabs = false, initialKindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                isTeam = isTeam,
                onBack = { navController.popBackStack() }
            )
        }

        // Classement complet des ADVERSAIRES scopé à un joueur/équipe (#67 round 3).
        composable(
            route = "Statsfull/{userId}/{kind}/Opponents/{isTeam}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType },
                navArgument("isTeam") { type = NavType.BoolType }
            )
        ) {
            val userIdArg = it.arguments?.getString("userId")?.takeIf { id -> id != "me" }
            val kind = it.arguments?.getString("kind")
            val isTeam = it.arguments?.getBoolean("isTeam") == true
            PlayerOpponentsRankingScreen(
                viewModel = hiltViewModel(
                    key = "$userIdArg-opponents-$isTeam-$kind",
                    creationCallback = { factory: StatsFullViewModel.Factory ->
                        factory.create(userId = userIdArg, showTabs = false, initialKindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                isTeam = isTeam,
                onBack = { navController.popBackStack() }
            )
        }

        // Historique des wars filtré sur un joueur (#65), graphe racine (back → StatsFull, rule 14).
        // `userId` = id du joueur, ou « me » = joueur courant (résolu par le VM).
        composable(
            route = "Home/WarList/{userId}/{kind}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType }
            )
        ) {
            val userId = it.arguments?.getString("userId")
            val kind = it.arguments?.getString("kind")
            WarListScreen(
                viewModel = hiltViewModel(
                    key = "warlist-$userId-$kind",
                    creationCallback = { factory: WarListViewModel.Factory ->
                        factory.create(userId = userId, initialKindFilter = WarKindFilter.fromRouteSegment(kind))
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

        // « Voir par période » (#80), graphe racine (pas de bottombar, rule 17).
        composable(
            route = "Home/Period/{kind}",
            arguments = listOf(navArgument("kind") { type = NavType.StringType })
        ) {
            val kind = it.arguments?.getString("kind")
            PeriodScreen(
                viewModel = hiltViewModel(
                    key = "period-$kind",
                    creationCallback = { factory: PeriodViewModel.Factory ->
                        factory.create(initialKindFilter = WarKindFilter.fromRouteSegment(kind))
                    }
                ),
                onWarDetailsClick = { warDetails ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", warDetails)
                    navController.navigate("Home/WarDetails")
                },
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
            // L'argument sème le mode initial du VM ; la bascule 12/24 se fait en interne
            // (état réactif, sans re-navigation).
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
                // « Voir l'adversaire » → fiche adversaire. opponentId = rosterId (ou teamId
                // legacy) ; userId « null » = portée Équipe (rule 15).
                onOpponent = { opponentId ->
                    // Depuis une war : pas de contexte de saison → tout l'historique (« all », #91 pt.5).
                    navController.navigate("Opponent/$opponentId/null/all/${WarKindFilter().routeSegment}")
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

/** Décode le segment de route « saison » (#91 pt.5) : « all »/null → null (tout l'historique). */
private fun String?.toSeasonNumber(): Int? = this?.takeIf { it != "all" }?.toIntOrNull()