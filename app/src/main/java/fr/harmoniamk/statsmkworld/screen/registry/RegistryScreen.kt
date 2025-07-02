package fr.harmoniamk.statsmkworld.screen.registry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import kotlinx.coroutines.launch

@Composable
fun RegistryScreen(viewModel: RegistryViewModel = hiltViewModel(), onPlayerProfile: (String) -> Unit, onTeamProfile: (String) -> Unit) {
    val teamSearch = remember { mutableStateOf("") }
    val playerSearch = remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val state = viewModel.state.collectAsState()
    BaseScreen(title = stringResource(R.string.registre)) {

        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Colors.blackAlphaed),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val text = when (iteration) {
                    0 -> stringResource(R.string.joueurs)
                    else -> stringResource(R.string.equipes)
                }
                val bgColor = when (iteration == pagerState.currentPage) {
                    true -> Colors.blackAlphaed
                    else -> Colors.transparent
                }
                val textColor = when (iteration == pagerState.currentPage) {
                    true -> Colors.white
                    else -> Colors.black
                }

                Box(
                    Modifier
                        .weight(1f)
                        .background(bgColor)
                        .clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(iteration)
                            }
                        }) {
                    MKText(
                        text = text,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 10.dp),
                        font = Fonts.Urbanist,
                        textColor = textColor,
                        fontSize = 16,
                        maxLines = 1
                    )
                }

            }
        }
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
                                PlayerCell(player = PlayerEntity(it), onClick = { onPlayerProfile(it.id) })
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