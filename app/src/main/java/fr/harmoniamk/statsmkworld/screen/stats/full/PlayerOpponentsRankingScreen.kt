package fr.harmoniamk.statsmkworld.screen.stats.full

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius
import fr.harmoniamk.statsmkworld.ui.stats.podiumRows

/**
 * Classement complet des adversaires (#67). [isTeam] false → adversaires du joueur (score
 * joueur), true → adversaires d'équipe (écart). Réutilise le même `StatsFullViewModel`
 * (rules 16/32) et la grille `podiumRows`. Tri Occurrences / Winrate / Score moy.
 */
@Composable
fun PlayerOpponentsRankingScreen(
    viewModel: StatsFullViewModel,
    isTeam: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }

    // All-time (index 0) : destination autonome sans sélecteur de période (#68).
    val podiums = (if (isTeam) state.teamOpponentsByWindow[0] else state.playerOpponentsByWindow[0])
        ?: StatsFullViewModel.OpponentPodiums()
    // Tri + conversion mémoïsés (rule 11, #73).
    val rows = remember(sortIndex, podiums, isTeam) {
        podiums.all
            .let { list ->
                when (sortIndex) {
                    1 -> list.sortedByDescending { it.winratePercent }
                    2 -> list.sortedByDescending { it.stats.averagePoints }
                    else -> list.sortedByDescending { it.stats.warStats.warsPlayed }
                }
            }
            .map { opponent -> opponent.toPodiumEntry(isTeam) to opponent }
    }

    BaseScreen(title = stringResource(R.string.best_opponents_section), onBack = onBack) {
        when {
            state.loading -> CircularProgressIndicator()
            rows.isEmpty() -> MKText(text = stringResource(R.string.stats_no_data), textColor = Colors.white66, fontSize = 13)
            else -> {
                MKSegmentedSelector(
                    items = listOf(
                        stringResource(R.string.stats_sort_occurrences),
                        stringResource(R.string.stats_sort_winrate),
                        stringResource(R.string.stats_sort_score)
                    ),
                    page = sortIndex,
                    onClick = { sortIndex = it }
                )
                Spacer(Modifier.height(11.dp))
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(StatCardRadius)
                        .background(Colors.blackAlphaed, StatCardRadius)
                        .border(1.dp, Colors.whiteBorder, StatCardRadius)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    podiumRows(
                        items = rows,
                        contentColor = Colors.white
                    )
                }
            }
        }
    }
}

/**
 * Adversaire → entrée de podium. [isTeam] true ⇒ écart d'équipe ; false ⇒ score du joueur.
 * Rule 12 : nom/tag du roster, logo de l'équipe parente.
 */
private fun RankingItem.OpponentRanking.toPodiumEntry(isTeam: Boolean): PodiumEntry =
    PodiumEntry(
        name = team.name,
        logo = team.logo,
        stats = listOf(
            R.string.times_played_short to warsPlayedLabel,
            R.string.form_winrate to winrateLabel,
            R.string.form_score to if (isTeam) stats.averagePointsLabel else stats.averagePoints.toString()
        )
    )
