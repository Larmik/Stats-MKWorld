package fr.harmoniamk.statsmkworld.screen.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel
import fr.harmoniamk.statsmkworld.ui.stats.MKMapsStatsCell
import fr.harmoniamk.statsmkworld.ui.stats.MKPlayerScoreCell
import fr.harmoniamk.statsmkworld.ui.stats.MKTeamStatsView
import fr.harmoniamk.statsmkworld.ui.stats.MKTopBottomCell
import fr.harmoniamk.statsmkworld.ui.stats.MKWarDetailsStatsView
import fr.harmoniamk.statsmkworld.ui.stats.MKWarStatsView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state = viewModel.state.collectAsState()
    BaseScreen(title = stringResource(viewModel.type?.title ?: R.string.statistiques)) {
        when (state.value.mapStats == null && state.value.stats == null) {
            true -> CircularProgressIndicator()
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.fillMaxWidth(0.5f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.value.player?.let {
                        PlayerCell(modifier = Modifier.fillMaxWidth(), player = it, onClick = { })
                    }
                    state.value.team?.let {
                        TeamCell(modifier = Modifier.fillMaxWidth(), team = it, onClick = { })
                    }
                    state.value.map?.let {
                        MapCell(modifier = Modifier.fillMaxWidth(), map = it, onClick = { })
                    }
                }
                MKWarStatsView(state.value.stats, state.value.mapStats)
                MKWarDetailsStatsView(state.value.stats, state.value.mapStats, type = viewModel.type)
                if (viewModel.type !is StatsType.MapStats)
                    MKPlayerScoreCell(stats = state.value.stats, type = viewModel.type)
                if (viewModel.type is StatsType.OpponentStats) {
                    MKText(text = "Historique des wars")
                    state.value.stats?.warStats?.list?.safeSubList(0, 5)?.map {
                        WarCell(
                            modifier = Modifier.padding(vertical = 5.dp),
                            viewModel = hiltViewModel(
                                key = it.war.id.toString(),
                                creationCallback = { factory: WarCellViewModel.Factory ->
                                    factory.create(it)
                                }
                            ),
                            onClick = { }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (viewModel.type is StatsType.PlayerStats || viewModel.type is StatsType.TeamStats)
                    MKTeamStatsView(state.value.stats)
                if (viewModel.type !is StatsType.MapStats)
                    MKMapsStatsCell(stats = state.value.stats, type = viewModel.type)
                val tops = state.value.mapStats?.topsTable
                val bottoms = state.value.mapStats?.bottomsTable
                val indivTops = state.value.mapStats?.indivTopsTable
                val indivBottoms = state.value.mapStats?.indivBottomsTable
                if (tops.takeIf { it?.any { it.second > 0 } == true } != null && bottoms.takeIf { it?.any { it.second > 0 } == true } != null)
                    MKTopBottomCell(false, tops, bottoms)
                if (indivTops.takeIf { it?.any { it.second > 0 } == true } != null && indivTops.takeIf { it?.any { it.second > 0 } == true } != null)
                    MKTopBottomCell(true, indivTops, indivBottoms)

            }
        }
    }

}