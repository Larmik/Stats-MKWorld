package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
import fr.harmoniamk.statsmkworld.model.selectors.PlayerSelector
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKBottomSheet
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddWarScreen(
    viewModel: AddWarViewModel,
    is24p: Boolean,
    onModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onCurrentWar: () -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val searchTeam = remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val rosterSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.goToCurrent.collect {
            onCurrentWar()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.openRosterSheet.collect {
            rosterSheetState.show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.dismissRosterSheet.collect {
            rosterSheetState.hide()
        }
    }

    BackHandler {
        when  {
            rosterSheetState.isVisible -> scope.launch { rosterSheetState.hide() }
            pagerState.currentPage == 1 -> scope.launch { pagerState.animateScrollToPage(0) }
            state.value.teamSelected?.isNotEmpty() == true -> { viewModel.onRemoveTeam() }
            else -> onBack()
        }
    }
    MKBottomSheet(
        sheetState = rosterSheetState,
        sheetContent = {
            RosterSelectionSheet(
                selection = state.value.rosterSelection,
                onRosterSelected = viewModel::onRosterSelected,
                onValidate = viewModel::onRosterValidated
            )
        }
    ) {
    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        state = pagerState,
        userScrollEnabled = false
    ) {
        when (it) {
            0 -> BaseScreen(title = stringResource(R.string.pick_opponent)) {
                // Segmenté 12/24 : c'est ICI que vit le sélecteur de mode (déménagé
                // de l'Accueil vers le pôle Wars). Le changer relance la création
                // de war dans l'autre mode (VM recréé via onModeChange).
                MKSegmentedSelector(
                    items = listOf(
                        stringResource(R.string.mode_12_players),
                        stringResource(R.string.mode_24_players)
                    ),
                    page = if (is24p) 1 else 0,
                    onClick = { selected ->
                        val newIs24p = selected == 1
                        if (newIs24p != is24p) onModeChange(newIs24p)
                    }
                )
                Spacer(Modifier.height(15.dp))
                if (is24p)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OpponentSlot(team = state.value.opponentSlot(0))
                        OpponentSlot(team = state.value.opponentSlot(1))
                        OpponentSlot(team = state.value.opponentSlot(2))
                    }
                else
                    OpponentSlot(team = state.value.opponentSlot(0))



                // Tant que la sélection des adversaires n'est pas terminée, on garde le
                // champ de recherche visible (même si la recherche ne renvoie aucune
                // équipe), pour ne pas perdre le clavier/focus. Une fois tous les
                // adversaires sélectionnés (nextButtonEnabled), la section disparaît.
                if (!state.value.nextButtonEnabled) {
                    MKTextField(
                        baseModifier = Modifier.semantics { contentDescription = "Recherche equipe" },
                        value = searchTeam.value,
                        onValueChange = {
                            searchTeam.value = it
                            viewModel.onSearchTeam(it)
                        },
                        placeHolderRes = R.string.search_team,
                        backgroundColor = Colors.blackAlphaed
                    )
                    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                        items(state.value.teamList, key = { it.id }) {
                            TeamCell(modifier = Modifier.padding(5.dp), team = it, onClick = {
                                viewModel.onTeamSelected(it)
                            })
                        }
                    }
                }
                MKButton(style = MKButtonStyle.Gradient, text = stringResource(R.string.next), enabled = state.value.nextButtonEnabled) {
                    scope.launch { pagerState.animateScrollToPage(1) }
                }
            }

            else -> BaseScreen(title = stringResource(R.string.pick_lu)) {
                state.value.warName?.let {
                    MKText(text = it, fontSize = 18)
                }
                LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    state.value.playerList.forEach { (rosterName, list) ->
                        stickyHeader {
                            AddWarRosterHeader(
                                text = when (rosterName.isEmpty()) {
                                    true -> stringResource(R.string.allies)
                                    else -> rosterName
                                }
                            )
                        }
                        item {
                            AddWarPlayerGrid(
                                players = list,
                                onPlayerSelected = viewModel::onPlayerSelected
                            )
                        }
                    }


            }

            MKButton(
                style = MKButtonStyle.Gradient,
                text = stringResource(R.string.commencer),
                enabled = state.value.buttonEnabled,
                onClick = viewModel::createWar
            )
        }
    }
    }

}

}

@Composable
private fun OpponentSlot(team: TeamEntity?) {
    team?.let {
        TeamCell(modifier = Modifier.size(120.dp), team = it, tagVisible = false) {}
    } ?: Spacer(
        Modifier
            .size(120.dp)
            .background(Colors.transparent)
            .border(2.dp, Colors.blackAlphaed, RoundedCornerShape(5.dp))
    )
}

@Composable
private fun RosterSelectionSheet(
    selection: AddWarViewModel.RosterSelection?,
    onRosterSelected: (MKCTeamRoster) -> Unit,
    onValidate: () -> Unit
) {
    selection ?: return
    BaseScreen(title = stringResource(R.string.pick_roster)) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            items(selection.rosters, key = { it.id }) { roster ->
                RosterCell(
                    roster = roster,
                    isSelected = roster.id == selection.selectedRoster?.id,
                    onClick = { onRosterSelected(roster) }
                )
            }
        }
        MKButton(
            style = MKButtonStyle.Gradient,
            text = stringResource(R.string.next),
            enabled = selection.selectedRoster != null,
            onClick = onValidate
        )
    }
}

@Composable
private fun RosterCell(
    roster: MKCTeamRoster,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (isSelected) {
        true -> Colors.whiteAlphaed
        else -> Colors.blackAlphaed
    }
    val textColor = when (isSelected) {
        true -> Colors.black
        else -> Colors.white
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(backgroundColor, RoundedCornerShape(5.dp))
            .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
            .clickable { onClick() }
    ) {
        MKText(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.Center),
            fontSize = 16,
            font = Fonts.NunitoBD,
            textColor = textColor,
            text = "${roster.name} (${roster.tag})"
        )
    }
}

@Composable
private fun AddWarRosterHeader(text: String) {
    Box(Modifier
        .fillMaxWidth()
        .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
        .border(1.dp, Colors.white, RoundedCornerShape(5.dp))) {
        MKText(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.Center),
            fontSize = 18,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            text = text
        )
    }
}

@Composable
private fun LazyItemScope.AddWarPlayerGrid(
    players: List<PlayerSelector>,
    onPlayerSelected: (PlayerEntity) -> Unit
) {
    VerticalGrid {
        players.forEach {
            val textColor = when (it.isSelected) {
                true -> Colors.black
                else -> Colors.white
            }
            val backgroundColor = when (it.isSelected) {
                true -> Colors.whiteAlphaed
                else -> Colors.blackAlphaed
            }
            PlayerCell(
                modifier = Modifier
                    .padding(5.dp)
                    .fillParentMaxWidth(0.48f),
                player = it.player,
                textColor = textColor,
                backgroundColor = backgroundColor,
                onClick = onPlayerSelected
            )
        }
    }
}