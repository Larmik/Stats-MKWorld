package fr.harmoniamk.statsmkworld.screen.addTrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.PositionCell
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun AddTrackScreen(viewModel: AddTrackViewModel = hiltViewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    val search = remember { mutableStateOf("") }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val state = viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onBack.collect {
            when (pagerState.currentPage) {
                0 -> onBack()
                1 -> scope.launch { pagerState.animateScrollToPage(0) }
                2, 3 -> scope.launch { pagerState.animateScrollToPage(1) }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.onNext.collect {
           scope.launch { pagerState.animateScrollToPage(it) }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.backToWar.collect {
           onBack()
        }
    }


    BackHandler {
      viewModel.onBack()
    }
    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        count = 3,
        state = pagerState,
        userScrollEnabled = false
    ) {
        when (it) {
            0 -> BaseScreen(title = stringResource(R.string.pick_circuit)) {
                MKTextField(
                    value = search.value,
                    onValueChange = {
                        search.value = it
                        viewModel.onSearch(it)
                    },
                    placeHolderRes = R.string.rechercher_un_circuit,
                    backgroundColor = Colors.blackAlphaed
                )
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                    items(state.value.mapList) {
                        MapCell(Modifier.padding(5.dp), map = listOf(it), onClick = {
                            viewModel.onMapSelected(it)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        })
                    }
                }
            }

            1 -> BaseScreen(title = stringResource(R.string.pick_position), subtitle = stringResource(
                R.string.current_race, state.value.trackOrder.toString()
            )) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.value.mapSelected?.let {
                        MapCell(map = it, onClick =  { })
                    }
                }
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    state.value.currentPlayer?.let {
                        MKText(text = it.name, fontSize = 24, font = Fonts.NunitoBD, modifier = Modifier.padding(bottom = 10.dp))
                    }

                    state.value.totalPositions?.let { total ->
                        val size = when (total) {
                            12 -> 120.dp
                            else -> 80.dp
                        }
                        LazyVerticalGrid(columns = GridCells.Adaptive(size)) {
                            items(total) {
                                PositionCell(
                                    position = it+1,
                                    is24p = total == 24,
                                    modifier = Modifier
                                        .size(size)
                                        .padding(5.dp), isVisible = !state.value.selectedPositions.map { it.position.position }.contains(it+1), onClick = viewModel::onPositionClick)
                            }
                        }
                    }
                }

            }
            2 -> BaseScreen(title = stringResource(R.string.resume), modifier = Modifier.verticalScroll(rememberScrollState())) {
                state.value.mapSelected?.let {
                    MapCell(map = it, backgroundColor = Colors.transparent, textColor = Colors.black, borderColor = Colors.transparent, onClick = { })
                }
                VerticalGrid {
                    state.value.selectedPositions.forEach {
                        PlayerCell(player = it.player, position = it.position.position, modifier = Modifier.padding(5.dp), shocksEnabled = true, shockCount = state.value.shocks[it.player?.id], is24p = state.value.teamOpponent.orEmpty().size > 1, onAddShock = viewModel::onAddShock, onRemoveShock = viewModel::onRemoveShock, onClick = {} )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (state.value.totalPositions) {
                        12 -> {
                            MKText(text = state.value.trackScore.orEmpty(), fontSize = 32)
                            MKText(text = state.value.trackDiff.orEmpty(), fontSize = 24)
                        }
                        24 -> {
                            Row {
                                val score = state.value.scores.orEmpty().firstOrNull { it.teamId == state.value.rosterId }?.score ?: 0
                                MKText(text = score.toString(), fontSize = 32)
                                MKText(text = "  ->  ", fontSize = 32, font = Fonts.NunitoBD)
                                MKText(text = "${score + (state.value.teamHostTrackScore ?: 0)}", fontSize = 32, font = Fonts.NunitoBD)
                            }
                        }
                    }
                }
                MKButton(style = MKButtonStyle.Gradient, text = stringResource(R.string.confirmer), onClick = viewModel::onValidate)
                Spacer(Modifier.height(40.dp))
            }
        }
    }



}
