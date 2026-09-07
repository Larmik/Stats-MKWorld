package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.MapStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Sections détaillées d'une sélection de manches ([MapStats]), mutualisées (#27) par les fiches
 * Adversaire et Circuit (mêmes rendus que `StatsFullScreen`) : répartition des positions
 * (histogramme + pied Top6/Bot6) et Top/Bot 2→6. Chaque bloc = un `item`. Rien si la sélection est vide.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun LazyListScope.mapStatsDetailSections(mapStats: MapStats) {
    val distribution = mapStats.positionDistribution
    if (distribution.any { it.second > 0 }) item {
        StatCard(title = stringResource(R.string.stats_distribution_title_generic)) {
            DistributionChart(distribution)
            DistributionFooter(distribution)
        }
    }

    // Top/Bot 5→2 équipe — masqué si aucune ligne affichable (#64).
    if (hasDisplayableTopBottom(mapStats.topsTable, mapStats.bottomsTable)) item {
        StatCard(title = stringResource(R.string.stats_top_bottom_team_title)) {
            TopBottomColumns(tops = mapStats.topsTable, bottoms = mapStats.bottomsTable)
        }
    }

    // Top/Bot 5→2 adversaire (12p/vue équipe uniquement) — masqué si aucune ligne affichable.
    if (hasDisplayableTopBottom(mapStats.opponentTopsTable, mapStats.opponentBottomsTable)) item {
        StatCard(title = stringResource(R.string.stats_top_bottom_opponent_title)) {
            TopBottomColumns(tops = mapStats.opponentTopsTable, bottoms = mapStats.opponentBottomsTable)
        }
    }
}
