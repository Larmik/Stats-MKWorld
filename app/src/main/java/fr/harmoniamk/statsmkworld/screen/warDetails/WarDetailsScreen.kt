package fr.harmoniamk.statsmkworld.screen.warDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.WarPlayerRankingCard
import fr.harmoniamk.statsmkworld.ui.cells.WarScoreCard
import fr.harmoniamk.statsmkworld.ui.cells.WarTracksSection

/**
 * Détail d'une **war terminée** (pôle Wars, écran `wardetails` de la maquette 5 pôles,
 * ticket #48). Écran-frère de `CurrentWarScreen` : il en réutilise les composants de résumé
 * partagés ([WarScoreCard], [WarTracksSection] — rule 16). Rendu pixel-perfect (rules 13/15).
 *
 * Structure (maquette) :
 * 1. **Carte score** hôte VS adversaire(s) ([WarScoreCard], sans sous-titre — war terminée).
 * 2. **Classement joueurs** ([WarPlayerRankingCard]) : tuiles nom + points, classées par
 *    points décroissants.
 * 3. Deux **boutons** (`MKButton` à icône) : « Générer le Tab (PDF) » (→ EditTab, **12 j / 1v1
 *    uniquement** — masqué en 24 j) et « Voir l'adversaire » (→ fiche adversaire).
 * 4. **Courses jouées · N** ([WarTracksSection]) : grille des courses, chacune → TrackDetails.
 *
 * Écran du **graphe racine** (poussé par-dessus WarList / CurrentWar / fiche adversaire) →
 * **pas de bottombar**, aucune marge basse requise (rule 17).
 *
 * Nom/tag = roster (rule 12) : résolus côté ViewModel (avatar équipe, nom/tag du roster).
 */
@Composable
fun WarDetailsScreen(
    viewModel: WarDetailsViewModel,
    onBack: () -> Unit,
    onTrackClick: (WarTrackDetails, Int) -> Unit,
    onTab: (WarDetails) -> Unit,
    onOpponent: (String) -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }
    BaseScreen(title = stringResource(R.string.wardetails_title), onBack = onBack, modifier = Modifier.fillMaxSize()) {
        state.value.details?.let { details ->
            val is24p = state.value.teamOpponent.orEmpty().size > 1
            val opponentId = details.war.teamOpponent.firstOrNull()
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // 1. Carte score : hôte VS adversaire(s), sans sous-titre (war terminée).
                item {
                    WarScoreCard(
                        teamHost = state.value.teamHost,
                        teamOpponent = state.value.teamOpponent,
                        details = details,
                        is24p = is24p
                    )
                }

                // 2. Classement joueurs (tuiles nom + points, triées par points décroissants).
                item {
                    WarPlayerRankingCard(
                        title = stringResource(R.string.wardetails_player_ranking),
                        players = state.value.players
                    )
                }

                // 3. Boutons : « Tab (PDF) » (12 j / 1v1 uniquement) + « Voir l'adversaire ».
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        if (!is24p) {
                            MKButton(
                                icon = R.drawable.ic_share,
                                text = stringResource(R.string.wardetails_generate_tab),
                                modifier = Modifier.weight(1f),
                                onClick = { onTab(details) }
                            )
                        }
                        if (opponentId != null) {
                            MKButton(
                                icon = R.drawable.ic_cup,
                                text = stringResource(R.string.wardetails_see_opponent),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpponent(opponentId) }
                            )
                        }
                    }
                }

                // 4. Courses jouées · N (grille, chacune → détail de la course).
                if (details.warTracks.isNotEmpty()) {
                    item {
                        WarTracksSection(
                            tracks = details.warTracks,
                            is24p = is24p,
                            onTrackDetails = onTrackClick
                        )
                    }
                }
            }
        }
    }
}

