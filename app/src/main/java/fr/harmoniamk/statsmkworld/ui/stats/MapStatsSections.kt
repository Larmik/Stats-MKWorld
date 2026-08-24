package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.MapStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Sections « détaillées » d'une sélection de manches ([MapStats]), **mutualisées** par
 * les fiches détail Adversaire (scopé adversaire) et Circuit (scopé circuit) — #27. Elles
 * reprennent les mêmes calculs/rendus que l'écran Statistiques (`StatsFullScreen`) :
 *
 * 1. **Répartition des positions** — histogramme P1→P12 ([DistributionChart]) + pied
 *    Top6/Bot6 ([DistributionFooter]), sur les positions du joueur (indiv) ou de l'ÉQUIPE ;
 * 2. **Top / Bot** — compteurs Top 2→6 et Bot 2→6 ([TopBottomColumns]).
 *
 * (Les shocks ne sont PLUS une section autonome : ils sont intégrés à « Séries & scores »
 * de la fiche adversaire / aux « Scores moyens » de la fiche circuit.)
 *
 * Chaque bloc est ajouté comme un `item` distinct (espacement vertical du LazyColumn hôte
 * conservé). Rien n'est ajouté si la sélection est vide.
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

    // Top/Bot 2→6 (compteurs d'ÉQUIPE) — masqué si aucune occurrence.
    if (mapStats.topsTable.any { it.second > 0 } || mapStats.bottomsTable.any { it.second > 0 }) item {
        StatCard(title = stringResource(R.string.stats_top_bottom_team_title)) {
            TopBottomColumns(tops = mapStats.topsTable, bottoms = mapStats.bottomsTable)
        }
    }

    // Top/Bot 2→6 de l'ADVERSAIRE (complément des positions, 12p/vue équipe uniquement —
    // les tables sont neutralisées à 0 sinon) — masqué si aucune occurrence.
    if (mapStats.opponentTopsTable.any { it.second > 0 } || mapStats.opponentBottomsTable.any { it.second > 0 }) item {
        StatCard(title = stringResource(R.string.stats_top_bottom_opponent_title)) {
            TopBottomColumns(tops = mapStats.opponentTopsTable, bottoms = mapStats.opponentBottomsTable)
        }
    }
}
