package fr.harmoniamk.statsmkworld.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.screen.addTrack.AddTrackScreen
import fr.harmoniamk.statsmkworld.screen.addWar.AddWarScreen
import fr.harmoniamk.statsmkworld.screen.currentWar.CurrentWarActionsScreen
import fr.harmoniamk.statsmkworld.screen.currentWar.CurrentWarScreen
import fr.harmoniamk.statsmkworld.screen.editTrack.EditTrackScreen
import fr.harmoniamk.statsmkworld.screen.editTrack.EditTrackViewModel
import fr.harmoniamk.statsmkworld.screen.home.HomeScreen
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileScreen
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileViewModel
import fr.harmoniamk.statsmkworld.screen.signup.SignupScreen
import fr.harmoniamk.statsmkworld.screen.signup.SignupViewModel
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileScreen
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileViewModel
import fr.harmoniamk.statsmkworld.screen.trackDetails.TrackDetailsScreen
import fr.harmoniamk.statsmkworld.screen.trackDetails.TrackDetailsViewModel
import fr.harmoniamk.statsmkworld.screen.warDetails.WarDetailsScreen
import fr.harmoniamk.statsmkworld.screen.warDetails.WarDetailsViewModel

@Composable
fun RootScreen(startDestination: String, code: String = "", currentPage: Int?, onBack: () -> Unit) {
    val navController = rememberNavController()
    NavHost(modifier = Modifier.fillMaxSize(), navController = navController, startDestination = startDestination) {

        composable(route = "Signup") {
            SignupScreen(
                viewModel = hiltViewModel(
                    key = code,
                    creationCallback = { factory: SignupViewModel.Factory ->
                        factory.create(code)
                    }
                ),
                currentPage = currentPage,
                onBack = onBack,
                onNext = { navController.navigate("Home") }
            )
        }

        composable(route = "Home") {
            HomeScreen(
                onBack = onBack,
                onTeamProfile = { navController.navigate("Team/Profile/$it") },
                onPlayerProfile = { navController.navigate("Player/Profile/$it") },
                onAddWar = { navController.navigate("Home/AddWar")},
                onCurrentWar = { navController.navigate("Home/CurrentWar")},
                onWarDetailsClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("war", it)
                    navController.navigate("Home/WarDetails")
                }
            )
        }

        composable(route = "Player/Profile/{id}",
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
                onDisconnect = { navController.navigate("Signup")}
            )
        }

        composable(route = "Team/Profile/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val id = it.arguments?.getString("id")
            TeamProfileScreen(viewModel =  hiltViewModel(
                key = id.toString(),
                creationCallback = { factory: TeamProfileViewModel.Factory ->
                    factory.create(id.toString())
                }),
                onBack = { navController.popBackStack() },
                onPlayerClick = { navController.navigate("Player/Profile/$it") },
            )
        }

        composable(route = "Home/AddWar") {
            AddWarScreen(onBack = {
                navController.popBackStack()
            }, onCurrentWar = {
                navController.popBackStack()
                navController.navigate(route =  "Home/CurrentWar")
            })
        }

        composable(route = "Home/CurrentWar") {
            CurrentWarScreen (
                onBack = { navController.popBackStack() },
                onAddTrack = { navController.navigate(route = "Home/CurrentWar/AddTrack")},
                onActions = { navController.navigate("Home/CurrentWar/Actions")},
                onTrackDetails = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("track", it)
                    navController.navigate("Home/TrackDetails/true")
                }
            )
        }

        composable(route = "Home/CurrentWar/AddTrack") {
            AddTrackScreen(onBack = { navController.popBackStack() })
        }

        composable(route = "Home/CurrentWar/Actions") {
            CurrentWarActionsScreen(onBack = { navController.popBackStack() }, onBackToWelcome = {
                navController.navigate(route = "Home")
            })
        }

        composable(route = "Home/WarDetails") {
            val war = navController.previousBackStackEntry?.savedStateHandle?.get<WarDetails>("war")
            WarDetailsScreen (
                viewModel = hiltViewModel(
                    key = war?.war?.id.toString(),
                    creationCallback = { factory:  WarDetailsViewModel.Factory ->
                        factory.create(war)
                    }
                ),
                onBack = { navController.popBackStack() },
                onTrackClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("track", it)
                    navController.navigate("Home/TrackDetails/false")
                }
            )
        }

        composable(route = "Home/TrackDetails/{editing}",
            arguments = listOf(navArgument("editing") { type = NavType.BoolType })

        ) {
            val track = navController.previousBackStackEntry?.savedStateHandle?.get<WarTrackDetails>("track")
            val editing = it.arguments?.getBoolean("editing") == true
            TrackDetailsScreen (
                viewModel = hiltViewModel(
                    key = track?.track?.id.toString(),
                    creationCallback = { factory:  TrackDetailsViewModel.Factory ->
                        factory.create(track, editing)
                    }
                ),
                onBack = { navController.popBackStack() },
                onEditTrack = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("track", it)
                    navController.navigate("Home/EditTrack")
                }
            )
        }

        composable(route = "Home/EditTrack") {
            val track = navController.previousBackStackEntry?.savedStateHandle?.get<WarTrackDetails>("track")
            EditTrackScreen(
                viewModel = hiltViewModel(
                    key = track?.track?.id.toString(),
                    creationCallback = { factory:  EditTrackViewModel.Factory ->
                        factory.create(track)
                    }
                ),
                onBack = { navController.popBackStack() },
                onBackToCurrent = { navController.navigate("Home/CurrentWar") },
            )
        }
    }
}