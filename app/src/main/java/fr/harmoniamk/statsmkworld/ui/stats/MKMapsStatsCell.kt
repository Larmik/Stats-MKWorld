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
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MapCell

@Composable
fun MKMapsStatsCell(stats: Stats?, type: StatsType?) {

    val userId = (type as? StatsType.PlayerStats)?.userId

    val bestMap = when (userId) {
        null -> stats?.bestMap
        else -> stats?.bestPlayerMap
    }
    val worstMap = when (userId) {
        null -> stats?.worstMap
        else -> stats?.worstPlayerMap
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
    ) {
        stats?.mostPlayedMap?.let {
            MKText(text = "Circuit le plus joué", modifier = Modifier.padding(bottom = 5.dp))
            MapCell(
                Modifier.fillMaxWidth(0.5f),
                track = null,
                isIndiv = userId != null,
                trackRanking = RankingItem.TrackRanking(it),
                onClick = {}
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
        bestMap?.let {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = "Meilleur circuit", modifier = Modifier.padding(bottom = 5.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp)) {
                    MapCell(
                        Modifier.fillMaxWidth(),
                        track = null,
                        isIndiv = userId != null,
                        trackRanking = RankingItem.TrackRanking(it),
                        onClick = {}
                    )
                }
            }
        }
        worstMap?.let {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = "Pire circuit", modifier = Modifier.padding(bottom = 5.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp)) {
                    MapCell(
                        Modifier.fillMaxWidth(),
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