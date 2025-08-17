package fr.harmoniamk.statsmkworld.screen.registry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import kotlinx.coroutines.launch

@Composable
fun RegistryScreen(
    viewModel: RegistryViewModel = hiltViewModel(),
    onPlayerProfile: (String) -> Unit,
    onTeamProfile: (String) -> Unit
) {
    val teamSearch = remember { mutableStateOf("") }
    val playerSearch = remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val state = viewModel.state.collectAsState()
    BaseScreen(title = stringResource(R.string.registre)) {

        MKSegmentedSelector(
            items = listOf(stringResource(R.string.joueurs), stringResource(R.string.equipes)),
            page = pagerState.currentPage,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            }
        )

        HorizontalPager(
            beyondViewportPageCount = 2,
            state = pagerState
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (pagerState.currentPage) {
                    0 -> {
                        MKTextField(
                            backgroundColor = Colors.blackAlphaed,
                            placeHolderRes = R.string.rechercher_un_joueur,
                            value = playerSearch.value,
                            onValueChange = {
                                playerSearch.value = it
                                viewModel.onSearchPlayers(it)
                            }
                        )

                        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                            items(state.value.playerList) {
                                PlayerCell(
                                    player = PlayerEntity(it),
                                    onClick = { onPlayerProfile(it.id) })
                            }
                        }
                    }


                    else -> {
                        MKTextField(
                            backgroundColor = Colors.blackAlphaed,
                            placeHolderRes = R.string.search_team,
                            value = teamSearch.value,
                            onValueChange = {
                                teamSearch.value = it
                                viewModel.onSearchTeams(it)
                            }
                        )
                        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                            items(state.value.teamList) {
                                TeamCell(team = it, onClick = { onTeamProfile(it.id) })
                            }
                        }

                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun RegistryPreview() {
    RegistryScreen(onPlayerProfile = {}) {

    }
}