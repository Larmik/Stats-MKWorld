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
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius
import fr.harmoniamk.statsmkworld.ui.stats.podiumRows

/**
 * Classement COMPLET des circuits (« Classement entier » des podiums Circuits de
 * `StatsFullScreen`, #67 round 3) — **scopé au périmètre de l'écran d'origine** : [isTeam]
 * = false → circuits DU JOUEUR (position moyenne), true → circuits d'ÉQUIPE (écart d'équipe).
 * Réutilise le **même `StatsFullViewModel`** (même userId → mêmes données scopées, rules 16/32)
 * et la grille `podiumRows` mutualisée. Sélecteur de tri Occurrences / Winrate / Score moy.
 * (état local `rememberSaveable`, rule 11).
 */
@Composable
fun PlayerMapsRankingScreen(
    viewModel: StatsFullViewModel,
    isTeam: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }

    // Classement ENTIER = all-time (index 0) : cet écran est une destination autonome sans
    // sélecteur de période (le filtre global de #68 ne concerne que le pôle Stats).
    val stats = if (isTeam) state.teamStatsByWindow[0] else state.playerStatsByWindow[0]
    val userId = if (isTeam) null else state.targetUserId
    // Tri + conversion vers PodiumEntry mémoïsés (rule 11) : le corps composable ne les
    // recalcule plus à chaque recomposition, seulement quand le tri ou la source change (#73).
    val rows = remember(sortIndex, stats, userId) {
        stats?.maps.orEmpty()
            // Seuls les circuits réellement joués figurent au classement.
            .filter { it.totalPlayed > 0 }
            .let { list ->
                when (sortIndex) {
                    1 -> list.sortedByDescending { it.winRate ?: 0 }
                    2 -> list.sortedByDescending { (if (userId != null) it.playerScore else it.teamScore) ?: 0 }
                    else -> list.sortedByDescending { it.totalPlayed }
                }
            }
            .map { track -> track.toPodiumEntry(userId) to track }
    }

    BaseScreen(title = stringResource(R.string.best_maps_section), onBack = onBack, modifier = Modifier.padding(bottom = 90.dp)) {
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
 * Circuit → entrée de podium (illustration + Nb joué + Winrate + position joueur / écart
 * équipe) — même convention que les podiums Circuits de `StatsFullScreen`. [userId] non-null
 * ⇒ position moyenne du joueur ; null ⇒ écart d'équipe (`trackScoreToDiff`).
 */
private fun TrackStats.toPodiumEntry(userId: String?): PodiumEntry {
    val map = map?.firstOrNull()
    val scoreLabel = if (userId != null) R.string.average_position_short else R.string.form_score
    val scoreValue = when {
        userId != null -> playerScore.pointsToPosition(false).firstOrNull()?.toString() ?: "-"
        else -> teamScore?.trackScoreToDiff(false) ?: "-"
    }
    return PodiumEntry(
        labelRes = map?.label,
        pictureRes = map?.picture,
        stats = listOf(
            R.string.times_played_short to totalPlayed.toString(),
            R.string.form_winrate to "${winRate ?: 0}%",
            scoreLabel to scoreValue
        )
    )
}
