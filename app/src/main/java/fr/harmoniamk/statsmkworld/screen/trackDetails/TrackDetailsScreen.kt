package fr.harmoniamk.statsmkworld.screen.trackDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.diffColor
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.InfoTile
import fr.harmoniamk.statsmkworld.ui.stats.InfoTilesGrid
import fr.harmoniamk.statsmkworld.ui.stats.StatCard

/**
 * Relecture **en lecture seule** d'une course jouée (pôle Wars, écran `trackdetails` de la
 * maquette 5 pôles, ticket #47). Rendu pixel-perfect vs la maquette (rules 13/15) :
 *
 * 1. **Carte en-tête** ([TrackHeaderCard]) : illustration du circuit + nom (Bungee) + sous-titre
 *    « Course N · Score X (±diff) » (diff colorisée vert/rouge/blanc). Style aligné sur la carte
 *    en-tête du Résumé d'AddTrack (cohérence de l'epic).
 * 2. **Carte « Positions & shocks »** : grille 2 colonnes de tuiles `.two > .b` (nom + `P{n} · X
 *    shock(s)`), via [InfoTilesGrid] mutualisée avec le pôle Profil (rule 16). Lecture seule.
 * 3. Bouton **« Éditer la course »** → EditTrack, visible seulement si la war est en cours,
 *    l'édition autorisée, et la course **non finale** (cf. [TrackDetailsViewModel]).
 *
 * Écran du graphe racine (poussé par-dessus CurrentWar / WarDetails) → **pas de bottombar**,
 * aucune marge basse requise (rule 17).
 */
@Composable
fun TrackDetailsScreen(
    viewModel: TrackDetailsViewModel,
    onBack: () -> Unit,
    onEditTrack: (WarTrackDetails, Boolean) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }
    BaseScreen(title = stringResource(R.string.trackdetails_title), modifier = Modifier.fillMaxSize()) {
        state.track?.let { track ->
            TrackHeaderCard(track = track, courseNumber = state.courseNumber, hostScore = state.hostScore, diff = state.diff)
            Spacer(Modifier.height(9.dp))

            StatCard(title = stringResource(R.string.trackdetails_positions_shocks)) {
                // Une tuile par joueur, triée par position (comme la maquette : P1, P3, P5…).
                val tiles = state.positions
                    .sortedBy { it.position.position }
                    .map { playerPosition ->
                        val shockCount = state.shocks[playerPosition.player?.id] ?: 0
                        InfoTile(
                            key = playerPosition.player?.name.orEmpty(),
                            value = "P${playerPosition.position.position}",
                            valueSmall = "· ${pluralStringResource(R.plurals.trackdetails_shock_count, shockCount, shockCount)}"
                        )
                    }
                InfoTilesGrid(tiles)
            }

            if (state.buttonVisible) {
                Spacer(Modifier.height(9.dp))
                MKButton(
                    modifier = Modifier.fillMaxWidth(),
                    style = MKButtonStyle.Gradient,
                    text = stringResource(R.string.trackdetails_edit),
                    onClick = { onEditTrack(track, state.trackScore != null) }
                )
            }
        }
    }
}

/**
 * Carte en-tête : illustration du dernier circuit de la manche + nom (Bungee) + sous-titre
 * « Course N · Score X (±diff) ». La diff est colorisée (vert/rouge/blanc via [Int.diffColor]) ;
 * en 24 j (plusieurs adversaires) le score/diff par manche n'est pas affiché.
 */
@Composable
private fun TrackHeaderCard(
    track: WarTrackDetails,
    courseNumber: Int,
    hostScore: Int,
    diff: String?
) {
    // `index` peut lister plusieurs circuits (intermission) : on illustre par le dernier.
    val lastMap = track.index.lastOrNull()?.toIntOrNull()?.let { Maps.entries.getOrNull(it) }
    val diffValue = diff?.toIntOrNull() ?: 0
    StatCard {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            lastMap?.let {
                Image(
                    painter = painterResource(it.picture),
                    contentDescription = null,
                    modifier = Modifier.width(64.dp).height(44.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Column(Modifier.weight(1f)) {
                lastMap?.let {
                    MKText(text = stringResource(it.label), font = Fonts.Bungee, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start, maxLines = 2)
                }
                // Sous-titre « Course N · Score X (±diff) » — la diff est colorisée (12 j uniquement ;
                // en 24 j, pas de diff par manche, l'adversaire étant saisi ailleurs).
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    MKText(
                        text = stringResource(R.string.trackdetails_course_score, courseNumber, hostScore),
                        textColor = Colors.white66,
                        fontSize = 12
                    )
                    if (!track.is24p) {
                        MKText(
                            text = " (${diff.orEmpty()})",
                            font = Fonts.NunitoBD,
                            textColor = diffValue.diffColor(),
                            fontSize = 12
                        )
                    }
                }
            }
        }
    }
}
