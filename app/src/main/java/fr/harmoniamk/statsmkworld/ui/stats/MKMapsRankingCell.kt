package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MapCell

/**
 * Lot B/C — section accordéon « Meilleurs / pires circuits », par lignes
 * (top3 / flop3) sur DOUBLE critère : winrate ET score moyen affichés côte à
 * côte (chaque MapCell montre déjà les deux). Seuil ≥ 3 matchs appliqué en amont
 * (cf. [Stats.MIN_RANKING_SAMPLE]).
 */
@Composable
fun MKMapsRankingCell(stats: Stats?, type: StatsType?) {
    stats?.takeIf { it.topMapsByWinrate.isNotEmpty() }?.let {
        val userId = (type as? StatsType.PlayerStats)?.userId
            ?: (type as? StatsType.OpponentStats)?.userId
        val is24p = type?.is24PEnabled == true

        MKExpandableSection(title = stringResource(R.string.best_maps_section)) {
            MapRankingBlock(stringResource(R.string.best_winrate_maps), it.topMapsByWinrate, userId, is24p)
            MapRankingBlock(stringResource(R.string.worst_winrate_maps), it.flopMapsByWinrate, userId, is24p)
            MapRankingBlock(stringResource(R.string.best_score_maps), it.topMapsByScore, userId, is24p)
            MapRankingBlock(stringResource(R.string.worst_score_maps), it.flopMapsByScore, userId, is24p)
        }
    }
}

@Composable
private fun MapRankingBlock(title: String, maps: List<TrackStats>, userId: String?, is24p: Boolean) {
    if (maps.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        MKText(
            text = title,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 14,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            maps.forEach { track ->
                MapCell(
                    modifier = Modifier.weight(1f),
                    track = null,
                    userId = userId,
                    is24p = is24p,
                    trackRanking = RankingItem.TrackRanking(track),
                    onClick = {}
                )
            }
        }
    }
}
