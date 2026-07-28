package fr.harmoniamk.statsmkworld.screen.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileContent
import fr.harmoniamk.statsmkworld.screen.playerProfile.PlayerProfileViewModel
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileContent
import fr.harmoniamk.statsmkworld.screen.teamProfile.TeamProfileViewModel
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKBottomSheet
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import kotlinx.coroutines.launch

/**
 * Pôle **Profil** (ticket #28) — profil unique à **onglets fusionnés Joueur / Équipe**
 * (écran `profile` du prototype, cf. `docs/PROTOTYPE_UX.md`). Un seul écran, un seul
 * `BaseScreen`, un segmented partagé ([MKSegmentedSelector]) bascule dynamiquement
 * entre les deux onglets (état interne réactif, sans re-navigation — rule 11/14).
 *
 * Le contenu de chaque onglet **réutilise** le contenu existant des fiches profil :
 * - onglet Joueur → [PlayerProfileContent] (identité, infos, équipe, réglages,
 *   règles métier ally/rôle, entrée Debug) ;
 * - onglet Équipe → [TeamProfileContent] (logo, roster, alliés + ajout).
 *
 * Les deux profils portent sur « moi » / mon équipe (`id = "me"`). Le sheet « Ajouter
 * un ally » est hébergé ici (au-dessus des deux onglets).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDebug: () -> Unit,
    onOnboarding: () -> Unit
) {
    val playerViewModel: PlayerProfileViewModel = hiltViewModel(
        key = "me-player-profile",
        creationCallback = { factory: PlayerProfileViewModel.Factory -> factory.create("me") }
    )
    val teamViewModel: TeamProfileViewModel = hiltViewModel(
        key = "me-team-profile",
        creationCallback = { factory: TeamProfileViewModel.Factory -> factory.create("me") }
    )

    // 0 = Joueur, 1 = Équipe. État interne réactif (rule 11) : le segmented bascule
    // l'onglet sans re-navigation. Survit à la rotation.
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val teamState = teamViewModel.state.collectAsStateWithLifecycle()
    val playerSearch = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        teamViewModel.onDismiss.collect { bottomSheetState.hide() }
    }

    MKBottomSheet(
        sheetState = bottomSheetState,
        onBack = onBack,
        sheetContent = {
            BaseScreen(title = stringResource(R.string.ajouter_un_ally)) {
                MKTextField(
                    backgroundColor = Colors.blackAlphaed,
                    placeHolderRes = R.string.rechercher_un_joueur,
                    value = playerSearch.value,
                    onValueChange = {
                        playerSearch.value = it
                        teamViewModel.onSearchPlayers(it)
                    }
                )
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                    items(teamState.value.playerList, key = { it.id }) { player ->
                        PlayerCell(
                            player = PlayerEntity(player, isAlly = false),
                            onClick = { teamViewModel.addAlly(player) }
                        )
                    }
                }
            }
        }
    ) {
        BaseScreen(title = stringResource(R.string.profil)) {
            MKSegmentedSelector(
                items = listOf(
                    stringResource(R.string.profile_tab_player),
                    stringResource(R.string.profile_tab_team)
                ),
                page = tabIndex,
                onClick = { tabIndex = it }
            )
            Spacer(Modifier.height(11.dp))
            when (tabIndex) {
                1 -> TeamProfileContent(
                    viewModel = teamViewModel,
                    onPlayerClick = onPlayerClick,
                    onAddAllyClick = { scope.launch { bottomSheetState.show() } }
                )
                else -> PlayerProfileContent(
                    viewModel = playerViewModel,
                    onDisconnect = onDisconnect,
                    onDebug = onDebug,
                    onOnboarding = onOnboarding
                )
            }
        }
    }
}
