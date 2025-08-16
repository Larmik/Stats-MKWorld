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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun StatsMenuScreen(viewModel: StatsMenuViewModel = hiltViewModel(), onClick: (StatsType) -> Unit, onRanking: (StatsType?) -> Unit) {
    val state = viewModel.state.collectAsState()
    BaseScreen(title = "Statistiques") {
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(StatsType.PlayerStats(state.value.currentPlayerId.orEmpty())) }) {
                    MKText(
                        text = "Statistiques individuelles",
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(StatsType.TeamStats()) }) {
                    MKText(
                        text = "Statistiques de l'équipe",
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRanking(StatsType.TeamStats()) }) {
                    MKText(
                        text = "Statistiques des joueurs",
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRanking(StatsType.OpponentStats(state.value.currentTeamId.orEmpty())) }) {
                    MKText(
                        text = "Statistiques des adversaires",
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRanking(StatsType.MapStats(userId = state.value.currentPlayerId.orEmpty(), teamId = state.value.currentTeamId.orEmpty())) }) {
                    MKText(
                        text = "Statistiques des circuits",
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
        }


    }

}