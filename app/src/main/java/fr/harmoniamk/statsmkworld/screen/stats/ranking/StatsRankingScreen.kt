package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell

@Composable
fun StatsRankingScreen(
    viewModel: StatsRankingViewModel,
    onPlayerStats: (StatsType) -> Unit) {
    val state = viewModel.state.collectAsState()
    BaseScreen(title = state.value.title.orEmpty()) {


        VerticalGrid(Modifier.verticalScroll(rememberScrollState())) {
            state.value.list.forEach {
                when (it) {
                    is RankingItem.PlayerRanking ->  PlayerCell(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(0.48f),
                        textColor = Colors.white,
                        backgroundColor = Colors.blackAlphaed,
                        onClick = { onPlayerStats(StatsType.PlayerStats(it.id)) },
                        playerRanking = it,
                        player = null
                    )
                    is RankingItem.OpponentRanking -> TeamCell(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(0.48f),
                        teamRanking = it,
                        onClick = { onPlayerStats(StatsType.OpponentStats(teamId = it.id))},
                        team = null
                    )
                    is RankingItem.TrackRanking -> MapCell(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(0.48f),
                        trackRanking = it,
                        onClick = {}
                    )
                }

            }
        }

    }
}