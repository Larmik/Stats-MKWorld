package fr.harmoniamk.statsmkworld.screen.stats.opponent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * entier » de la fiche adversaire #27). Réutilise le même [OpponentDetailViewModel] (même
 * clé de nav → mêmes données, même mode ET même tri) et la grille `podiumRows` mutualisée
 * (rule 16). Sélecteur de tri Occurrences / Winrate / Score moy. en tête (point 5).
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
            else -> {
                // Sélecteur de tri sur le fond clair du BaseScreen (onDark false).
                TracksSortSelector(state.tracksSort, onDark = false, onSelect = viewModel::onTracksSortSelected)
                Spacer(Modifier.height(11.dp))
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    podiumRows(
                        // Texte des cellules en NOIR (point 9) sur le fond clair du BaseScreen.
                        items = state.allTracks.map { track -> track.toPodiumEntry(state.isIndiv) to track },
                        contentColor = Colors.black
                    )
                }
            }
        }
    }
}
