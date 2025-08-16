package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MapCell

@Composable
fun MKMapsStatsCell(
    stats: Stats?,
    type: StatsType?
) {

    val userId = (type as? StatsType.PlayerStats)?.userId

    val bestMap = when (userId) {
        null -> stats?.bestMap
        else -> stats?.bestPlayerMap
    }
    val worstMap = when (userId) {
        null -> stats?.worstMap
        else -> stats?.worstPlayerMap
    }

    Column {
        MKText(text = "Circuits", font = Fonts.NunitoBD, fontSize = 16)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            stats?.mostPlayedMap?.let {
                Column(Modifier.fillMaxWidth().padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(
                        text = "Le plus joué",
                        fontSize = 12,
                    )
                    Row(Modifier.fillMaxWidth(0.5f)) {
                        MapCell(
                            track = null,
                            isIndiv = userId != null,
                            trackRanking = RankingItem.TrackRanking(it),
                            onClick = {}
                        )
                    }

                }

            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    bestMap?.let {
                        MKText(
                            text = "Meilleur circuit",
                            fontSize = 12,
                        )
                        MapCell(
                            track = null,
                            isIndiv = userId != null,
                            trackRanking = RankingItem.TrackRanking(it),
                            onClick = {}
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    worstMap?.let {
                        MKText(
                            text = "Pire circuit",
                            fontSize = 12,
                        )
                        MapCell(
                            track = null,
                            isIndiv = userId != null,
                            trackRanking = RankingItem.TrackRanking(it),
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}