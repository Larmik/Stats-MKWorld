package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKDropdownMenu
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell

@Composable
fun StatsRankingScreen(
    viewModel: StatsRankingViewModel,
    onStats: (StatsType) -> Unit
) {
    val state = viewModel.state.collectAsState()
    val searchValue = remember { mutableStateOf("") }
    val expanded = remember { mutableStateOf(false) }

    BaseScreen(title = stringResource(state.value.title ?: R.string.statistiques)) {
        if (viewModel.type is StatsType.OpponentStats || viewModel.type is StatsType.MapStats) {
            MKSegmentedSelector(
                items = listOf(stringResource(R.string.individuel), stringResource(R.string.equipe)),
                page = state.value.index,
                onClick = viewModel::onIndivSwitch
            )
            Spacer(Modifier.height(10.dp))
            val placeHolder = when (viewModel.type) {
                is StatsType.OpponentStats -> R.string.search_team
                else -> R.string.rechercher_un_circuit
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MKTextField(baseModifier = Modifier.weight(1f), value = searchValue.value, onValueChange = {
                    searchValue.value = it
                    viewModel.onSearch(it)
                }, placeHolderRes = placeHolder)
                Box(Modifier.padding(start = 5.dp)) {
                    Image(
                        painter = painterResource(R.drawable.sort),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).clickable {
                            expanded.value = true
                        }
                    )
                    MKDropdownMenu(
                        expanded = expanded.value,
                        onDismiss = { expanded.value = false },
                        list = state.value.sortItems,
                        onSelectValue = viewModel::onSortItemSelected
                    )
                }

            }
            Spacer(Modifier.height(10.dp))
        }

        when (state.value.list.mapNotNull { it as? RankingItem.PlayerRanking }.isEmpty()) {
            true -> VerticalGrid(Modifier.verticalScroll(rememberScrollState())) {
                state.value.list.forEach {
                    when (it) {
                        is RankingItem.OpponentRanking -> TeamCell(
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(0.48f),
                            teamRanking = it,
                            onClick = { onStats(StatsType.OpponentStats(teamId = it.id, userId = state.value.currentUserId)) },
                            userId = state.value.currentUserId,
                            team = null
                        )
                        is RankingItem.TrackRanking -> MapCell(
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(0.48f),
                            trackRanking = it,
                            userId = state.value.currentUserId,
                            onClick = { onStats(StatsType.MapStats(userId = state.value.currentUserId, trackIndex = it.ordinal))}
                        )
                        else -> {}
                    }
                }
            }

            else -> LazyColumn {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                            .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
                    ) {
                        MKText(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.Center),
                            fontSize = 18,
                            font = Fonts.NunitoBD,
                            textColor = Colors.white,
                            text = stringResource(R.string.roster)
                        )
                    }
                }
                item {
                    VerticalGrid {
                        state.value.list
                            .mapNotNull { (it as? RankingItem.PlayerRanking) }
                            .filterNot { it.player.isAlly }
                            .forEach {
                                PlayerCell(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .fillMaxWidth(0.48f),
                                    textColor = Colors.white,
                                    backgroundColor = Colors.blackAlphaed,
                                    onClick = { onStats(StatsType.PlayerStats(it.id)) },
                                    playerRanking = it,
                                    player = null
                                )
                            }
                    }
                }
                item {
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
                            text = stringResource(R.string.allies)
                        )
                    }
                }
                item {
                    VerticalGrid {
                        state.value.list
                            .mapNotNull { (it as? RankingItem.PlayerRanking) }
                            .filter { it.player.isAlly }
                            .forEach {
                                PlayerCell(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .fillMaxWidth(0.48f),
                                    textColor = Colors.white,
                                    backgroundColor = Colors.blackAlphaed,
                                    onClick = { onStats(StatsType.PlayerStats(it.id)) },
                                    playerRanking = it,
                                    player = null
                                )
                            }
                    }
                }
            }
        }
    }
}