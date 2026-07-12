package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell

/**
 * Lot B/C — section accordéon « Meilleurs / pires adversaires », par lignes
 * (top3 / flop3), DOUBLE critère winrate ET score moyen (TeamCell affiche les
 * deux). Wording produit : « winrate/score moyen face à », jamais « meilleur
 * adversaire ». Seuil ≥ 3 matchs appliqué en amont (InitStatsWorker).
 */
@Composable
fun MKOpponentsRankingCell(
    topByWinrate: List<RankingItem.OpponentRanking>,
    flopByWinrate: List<RankingItem.OpponentRanking>,
    topByScore: List<RankingItem.OpponentRanking>,
    flopByScore: List<RankingItem.OpponentRanking>,
    // Non-null ⇒ vue joueur : TeamCell affiche alors le score du JOUEUR
    // (stats.averagePoints) et non l'écart d'équipe (averagePointsLabel), comme
    // dans l'écran StatsRankings.
    userId: String? = null
) {
    if (topByWinrate.isEmpty() && topByScore.isEmpty()) return
    MKExpandableSection(title = stringResource(R.string.best_opponents_section)) {
        OpponentRankingBlock(stringResource(R.string.best_winrate_vs), topByWinrate, userId)
        OpponentRankingBlock(stringResource(R.string.worst_winrate_vs), flopByWinrate, userId)
        OpponentRankingBlock(stringResource(R.string.best_score_vs), topByScore, userId)
        OpponentRankingBlock(stringResource(R.string.worst_score_vs), flopByScore, userId)
    }
}

@Composable
private fun OpponentRankingBlock(
    title: String,
    opponents: List<RankingItem.OpponentRanking>,
    userId: String?
) {
    if (opponents.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        MKText(
            text = title,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 14,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        // Une TeamCell par ligne (pleine largeur) : à 3 cellules côte à côte
        // (weight 1f), la largeur ~1/3 étouffe la colonne des valeurs de TeamCell
        // (wars/winrate/score) qui se retrouvait clippée à vide. En empilant, chaque
        // cellule a la largeur nécessaire pour afficher ses 3 valeurs.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            opponents.forEach { opponent ->
                TeamCell(
                    modifier = Modifier.fillMaxWidth(),
                    team = null,
                    teamRanking = opponent,
                    userId = userId,
                    onClick = {}
                )
            }
        }
    }
}
