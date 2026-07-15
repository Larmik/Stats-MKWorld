package fr.harmoniamk.statsmkworld.screen.stats.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText

/** Distingue les deux destinations issues de l'ancien menu unique : le détail des stats
 * (individuelles/équipe) et les classements (joueurs/adversaires/circuits). */
enum class StatsMenuMode { STATS, RANKINGS }

@Composable
fun StatsMenuScreen(
    viewModel: StatsMenuViewModel = hiltViewModel(),
    mode: StatsMenuMode,
    onClick: (StatsType) -> Unit,
    onRanking: (StatsType?) -> Unit,
    onSearch: (() -> Unit)? = null
) {
    val state = viewModel.state.collectAsState()
    val title = when (mode) {
        StatsMenuMode.STATS -> R.string.statistiques
        StatsMenuMode.RANKINGS -> R.string.classements
    }
    BaseScreen(title = stringResource(title), onSearch = onSearch) {
        state.value.is24PEnabled?.let {
            MKSegmentedSelector(
                items = listOf(
                    "12 joueurs",
                    "24 joueurs"
                ),
                page = when (it) {
                    true -> 1
                    else -> 0
                },
                onClick = viewModel::onWarTypeSwitch
            )
        }

        Spacer(Modifier.height(10.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (mode) {
                StatsMenuMode.STATS -> {
                    item {
                        MenuRow(text = stringResource(R.string.statistiques_individuelles)) {
                            onClick(StatsType.PlayerStats(userId = state.value.currentPlayerId.orEmpty(), is24p = state.value.is24PEnabled == true))
                        }
                    }
                    item {
                        MenuRow(text = stringResource(R.string.statistiques_de_l_quipe)) {
                            onClick(StatsType.TeamStats(is24p = state.value.is24PEnabled == true))
                        }
                    }
                }
                StatsMenuMode.RANKINGS -> {
                    item {
                        MenuRow(text = stringResource(R.string.statistiques_des_joueurs)) {
                            onRanking(StatsType.TeamStats(is24p = state.value.is24PEnabled == true))
                        }
                    }
                    item {
                        MenuRow(text = stringResource(R.string.statistiques_des_adversaires)) {
                            onRanking(StatsType.OpponentStats(teamId = state.value.currentTeamId.orEmpty(), is24p = state.value.is24PEnabled == true))
                        }
                    }
                    item {
                        MenuRow(text = stringResource(R.string.statistiques_des_circuits)) {
                            onRanking(
                                StatsType.MapStats(
                                    userId = state.value.currentPlayerId.orEmpty(),
                                    teamId = state.value.currentTeamId.orEmpty(),
                                    is24p = state.value.is24PEnabled == true
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        MKText(
            text = text,
            font = Fonts.Urbanist,
            modifier = Modifier.padding(vertical = 20.dp)
        )
    }
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Colors.blackAlphaed)
    )
}
