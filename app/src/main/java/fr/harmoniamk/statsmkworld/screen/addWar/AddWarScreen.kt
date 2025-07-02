package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddWarScreen(
    viewModel: AddWarViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCurrentWar: () -> Unit
) {
    val state = viewModel.state.collectAsState()
    val searchTeam = remember { mutableStateOf("") }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.goToCurrent.collect {
            onCurrentWar()
        }
    }

    BackHandler {
        when (pagerState.currentPage) {
            1 -> scope.launch { pagerState.animateScrollToPage(0) }
            else -> onBack()
        }
    }
    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        count = 2,
        state = pagerState,
        userScrollEnabled = false
    ) {
        when (it) {
            0 -> BaseScreen(title = stringResource(R.string.pick_opponent)) {
                MKTextField(
                    value = searchTeam.value,
                    onValueChange = {
                        searchTeam.value = it
                        viewModel.onSearchTeam(it)
                    },
                    placeHolderRes = R.string.search_team,
                    backgroundColor = Colors.blackAlphaed
                )
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                    items(state.value.teamList) {
                        TeamCell(modifier = Modifier.padding(5.dp), team = it, onClick = {
                            viewModel.onTeamSelected(it)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        })
                    }
                }
            }

            else -> BaseScreen(title = stringResource(R.string.pick_lu)) {
                state.value.warName?.let {
                    MKText(text = it, fontSize = 18)
                }
                LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    state.value.playerList.groupBy { it.player.isAlly }.forEach { (isAlly, list) ->
                        stickyHeader {
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
                                    text = when (isAlly) {
                                        true -> stringResource(R.string.allies)
                                        else -> stringResource(R.string.roster)
                                    }
                                )
                            }

                        }
                        item {
                            VerticalGrid {
                                list.forEach {
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
                                        onClick = viewModel::onPlayerSelected
                                    )
                                }
                            }
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