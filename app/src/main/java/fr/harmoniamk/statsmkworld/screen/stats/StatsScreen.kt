package fr.harmoniamk.statsmkworld.screen.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import fr.harmoniamk.statsmkworld.ui.stats.MKMapsStatsCell
import fr.harmoniamk.statsmkworld.ui.stats.MKPlayerScoreCell
import fr.harmoniamk.statsmkworld.ui.stats.MKTeamStatsView
import fr.harmoniamk.statsmkworld.ui.stats.MKWarDetailsStatsView
import fr.harmoniamk.statsmkworld.ui.stats.MKWarStatsView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state = viewModel.state.collectAsState()
    BaseScreen(title = viewModel.type?.title ?: "Statistiques") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            state.value.team?.let {
                TeamCell(team = it, onClick = { })
            }
            state.value.player?.let {
                PlayerCell(player = it, onClick = { })
            }
            MKWarStatsView(state.value.stats, state.value.mapStats)
            MKWarDetailsStatsView(state.value.stats, state.value.mapStats, type = viewModel.type)
            MKPlayerScoreCell(stats = state.value.stats, type = viewModel.type)
            if (viewModel.type !is StatsType.OpponentStats)
                MKTeamStatsView(state.value.stats)
            MKMapsStatsCell(stats = state.value.stats, type = viewModel.type)
        }
    }

}