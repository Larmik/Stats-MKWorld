package fr.harmoniamk.statsmkworld.screen.editTrack

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.PositionCell
import kotlinx.coroutines.launch

@Composable
fun EditTrackScreen(
    viewModel: EditTrackViewModel,
    onBack: () -> Unit,
    onBackToCurrent: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val state = viewModel.state.collectAsState()
    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        viewModel.backToCurrent.collect {
            onBackToCurrent()
        }
    }
    BaseScreen(title = stringResource(R.string.edition_circuit)) {
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Colors.blackAlphaed),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val text = when (iteration) {
                    0 -> stringResource(R.string.circuit)
                    1 -> stringResource(R.string.positions)
                    else -> stringResource(R.string.shocks)
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
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (pagerState.currentPage) {
                    0 -> {
                        when (state.value.mapList.isEmpty()) {
                            true -> CircularProgressIndicator()
                            else -> LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                                items(state.value.mapList) {
                                    val backgroundColor = when (state.value.mapSelected == it) {
                                        true -> Colors.whiteAlphaed
                                        else -> Colors.blackAlphaed
                                    }
                                    val textColor = when (state.value.mapSelected == it) {
                                        true -> Colors.black
                                        else -> Colors.white
                                    }
                                    MapCell(
                                        Modifier.padding(5.dp),
                                        map = it,
                                        textColor = textColor,
                                        backgroundColor = backgroundColor,
                                        onClick = viewModel::onMapSelected
                                    )
                                }
                            }
                        }

                    }
                    1 -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            when (state.value.players.isEmpty()) {
                                true -> CircularProgressIndicator()
                                else -> {
                                    state.value.currentPlayer?.let {
                                        MKText(
                                            text = it.name,
                                            fontSize = 24,
                                            font = Fonts.NunitoBD,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )
                                    }
                                    LazyVerticalGrid(columns = GridCells.Adaptive(120.dp)) {
                                        items(12) {
                                            PositionCell(
                                                position = it + 1,
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .padding(5.dp),
                                                isVisible = !state.value.selectedPositions.map { it.position.position }
                                                    .contains(it + 1),
                                                onClick = viewModel::onPositionClick
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                    else -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            VerticalGrid {
                                state.value.initialPositions.forEach {
                                    PlayerCell(player = it.player, position = it.position.position, modifier = Modifier.padding(5.dp), shocksEnabled = true, shockCount = state.value.shocks[it.player?.id], onAddShock = viewModel::onAddShock, onRemoveShock = viewModel::onRemoveShock) { }
                                }
                            }

                        }
                    }
                }
            }
        }
        Row(
            Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MKButton(
                style = MKButtonStyle.Gradient,
                text = stringResource(R.string.confirmer),
                enabled = state.value.buttonEnabled,
                onClick = viewModel::onValidate
            )
            MKButton(
                style = MKButtonStyle.Minor(Colors.black),
                text = stringResource(R.string.cancel),
                onClick = onBack
            )
        }
    }
}