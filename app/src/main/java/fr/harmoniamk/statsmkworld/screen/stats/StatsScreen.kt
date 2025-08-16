package fr.harmoniamk.statsmkworld.screen.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state = viewModel.state.collectAsState()

    BaseScreen(title = viewModel.type?.title ?: "Statistiques") {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            MKText(text = state.value.subtitle.orEmpty())
            //Pour indiv et team
            MKText(text = "Wars played : " + state.value.stats?.warStats?.warsPlayed.toString())
            MKText(text = "Wars won : " + state.value.stats?.warStats?.warsWon.toString())
            MKText(text = "Wars tied : " + state.value.stats?.warStats?.warsTied.toString())
            MKText(text = "Wars loss : " + state.value.stats?.warStats?.warsLoss.toString())
            MKText(text = "Maps won : " + state.value.stats?.mapsWon.toString())
            MKText(text = "Shock count : " + state.value.stats?.shockCount.toString())
            when (viewModel.type) {
                is StatsType.PlayerStats -> {
                    //Pour indiv seulement
                    MKText(text = "Highest score : " + state.value.stats?.highestScore?.score.toString())
                    MKText(text = "Lowest score : " + state.value.stats?.lowestScore?.score.toString())
                    MKText(text = "Best player map : " + state.value.stats?.bestPlayerMap?.map?.name.toString())
                    MKText(text = "Worst player map : " + state.value.stats?.worstPlayerMap?.map?.name.toString())
                    MKText(text = "Average position : " + state.value.stats?.averagePlayerPosition.toString())
                }
                is StatsType.TeamStats -> {
                    //Pour team seulement
                    MKText(text = "Highest victory : " + state.value.stats?.warStats?.highestVictory?.displayedScore.toString())
                    MKText(text = "Loudest defeat : " + state.value.stats?.warStats?.loudestDefeat?.displayedScore.toString())
                    MKText(text = "Most played map : " + state.value.stats?.mostPlayedMap?.map?.name.toString())
                    MKText(text = "Best team map : " + state.value.stats?.bestMap?.map?.name.toString())
                    MKText(text = "Worst team map : " + state.value.stats?.worstMap?.map?.name.toString())
                    MKText(text = "Average map points : " + state.value.stats?.averageMapPointsLabel.toString())
                    MKText(text = "Average score : " + state.value.stats?.averagePointsLabel.toString())

                }
                else -> {}
            }
        }
    }

}