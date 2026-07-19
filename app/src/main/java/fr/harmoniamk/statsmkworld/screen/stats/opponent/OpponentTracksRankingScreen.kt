package fr.harmoniamk.statsmkworld.screen.stats.opponent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.podiumRows
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Classement COMPLET des circuits joués contre un adversaire (« Voir le classement en
 * entier » de la fiche adversaire #27), par score moyen décroissant. Réutilise le même
 * [OpponentDetailViewModel] (même clé de nav → mêmes données, même mode Indiv/Équipe) et
 * la grille `podiumRows` mutualisée (rule 16).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun OpponentTracksRankingScreen(
    viewModel: OpponentDetailViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(title = stringResource(R.string.opponent_detail_best_tracks), modifier = Modifier.padding(bottom = 90.dp)) {
        when {
            state.loading -> CircularProgressIndicator()
            state.allTracks.isEmpty() -> MKText(text = stringResource(R.string.stats_no_data), textColor = Colors.white66, fontSize = 13)
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                podiumRows(
                    items = state.allTracks.map { track -> track.toPodiumEntry(state.isIndiv) to track },
                    contentColor = Colors.white
                )
            }
        }
    }
}
