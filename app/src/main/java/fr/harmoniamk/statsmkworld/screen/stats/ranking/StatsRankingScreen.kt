package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
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
    BaseScreen(title = state.value.title.orEmpty()) {
        if (viewModel.type is StatsType.OpponentStats || viewModel.type is StatsType.MapStats)
            MKSegmentedSelector(
                items = listOf("Individuel", "Equipe"),
                page = state.value.index,
                onClick = viewModel::onIndivSwitch
            )
        when (state.value.list.mapNotNull { it as? RankingItem.PlayerRanking }.isEmpty()) {
            true -> VerticalGrid(Modifier.verticalScroll(rememberScrollState())) {
                state.value.list.forEach {
                    when (it) {
                        is RankingItem.OpponentRanking -> TeamCell(
                            modifier = Modifier.padding(5.dp).fillMaxWidth(0.48f),
                            teamRanking = it,
                            onClick = { onStats(StatsType.OpponentStats(teamId = it.id, userId = state.value.currentUserId)) },
                            userId = state.value.currentUserId,
                            team = null
                        )
                        is RankingItem.TrackRanking -> MapCell(
                            modifier = Modifier.padding(5.dp).fillMaxWidth(0.48f),
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