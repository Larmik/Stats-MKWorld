package fr.harmoniamk.statsmkworld.screen.trackDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.diffColor
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius

/**
 * Relecture **en lecture seule** d'une course jouée (pôle Wars, écran `trackdetails` de la
 * maquette 5 pôles, ticket #47). Rendu pixel-perfect vs la maquette (rules 13/15) :
 *
 * 1. **Carte en-tête** ([TrackHeaderCard]) : illustration du circuit + nom (Bungee) + sous-titre
 *    « Course N · {score hôte - adverse} (±diff) » (diff colorisée vert/rouge/blanc). Style aligné
 *    sur la carte en-tête du Résumé d'AddTrack (cohérence de l'epic).
 * 2. **Carte « Positions & shocks »** : grille 2 colonnes de tuiles (nom + **position** rendue avec
 *    la font `MKPosition` colorée par [Int.positionColor], **chiffre seul**, + icône shock `x{n}`
 *    quand le joueur a ≥ 1 shock). Lecture seule.
 * 3. Bouton **« Éditer la course »** → EditTrack, visible tant que la war **n'est pas validée**
 *    (encore en cours) et que l'édition est autorisée (cf. [TrackDetailsViewModel]). Toutes les
 *    courses restent éditables tant que la war n'est pas validée (y compris la dernière).
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
    BaseScreen(title = stringResource(R.string.trackdetails_title), onBack = onBack, modifier = Modifier.fillMaxSize()) {
        state.track?.let { track ->
            TrackHeaderCard(track = track, courseNumber = state.courseNumber)
            Spacer(Modifier.height(9.dp))

            StatCard(title = stringResource(R.string.trackdetails_positions_shocks)) {
                // Une tuile par joueur, triée par position (comme la maquette : 1, 3, 5…).
                val players = state.positions.sortedBy { it.position.position }
                players.chunked(2).forEach { pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        pair.forEach { playerPosition ->
                            PositionShockTile(
                                playerPosition = playerPosition,
                                is24p = track.is24p,
                                shockCount = state.shocks[playerPosition.player?.id] ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (state.buttonVisible) {
                Spacer(Modifier.height(9.dp))
                MKButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.trackdetails_edit),
                    onClick = { onEditTrack(track, track.is24p) }
                )
            }
        }
    }
}

/**
 * Tuile joueur (lecture seule) de la grille « Positions & shocks » (`.two > .b` de la maquette) :
 * nom (petit, majuscule atténué) puis, sur une ligne, la **position** — chiffre seul rendu avec
 * la font `MKPosition` et coloré par [Int.positionColor] (comme partout dans l'app) — suivie de
 * l'icône shock + `x{n}` **uniquement** si le joueur a au moins un shock ([shockCount] > 0).
 */
@Composable
private fun PositionShockTile(
    playerPosition: PlayerPosition,
    is24p: Boolean,
    shockCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(Colors.white30, StatCardRadius)
            .padding(11.dp)
    ) {
        MKText(
            text = playerPosition.player?.name.orEmpty().displayName,
            font = Fonts.NunitoBD,
            fontSize = 10,
            textColor = Colors.white66,
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 5.dp)
        ) {
            MKText(
                text = playerPosition.position.position.toString(),
                font = Fonts.MKPosition,
                textColor = playerPosition.position.position.positionColor(is24p),
                fontSize = 26,
                resizable = false
            )
            // Icône shock + compteur, seulement s'il y a au moins un shock.
            if (shockCount > 0) {
                Image(
                    painter = painterResource(R.drawable.shock),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                MKText(
                    text = "x$shockCount",
                    font = Fonts.Urbanist,
                    textColor = Colors.white,
                    fontSize = 12,
                    resizable = false
                )
            }
        }
    }
}

/**
 * Carte en-tête : illustration du dernier circuit de la manche + nom (Bungee) + sous-titre
 * « Course N · {score hôte - adverse} (±diff) ». La diff est colorisée (vert/rouge/blanc via
 * [Int.diffColor]) ; en 24 j (plusieurs adversaires) le score/diff par manche n'est pas affiché.
 */
@Composable
private fun TrackHeaderCard(track: WarTrackDetails, courseNumber: Int) {
    // `index` peut lister plusieurs circuits (intermission) : on illustre par le dernier.
    val lastMap = track.index.lastOrNull()?.toIntOrNull()?.let { Maps.entries.getOrNull(it) }
    val diffValue = track.displayedDiff.toIntOrNull() ?: 0
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
                // Sous-titre « Course N · score hôte - adverse (±diff) » — le score des deux équipes
                // (WarTrackDetails.displayedResult) puis la diff colorisée (12 j uniquement ; en 24 j,
                // pas de score/diff par manche, l'adversaire étant saisi ailleurs).
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    MKText(
                        text = stringResource(R.string.trackdetails_course_prefix, courseNumber),
                        textColor = Colors.white66,
                        fontSize = 12
                    )
                    if (!track.is24p) {
                        MKText(text = " · ", textColor = Colors.white66, fontSize = 12)
                        MKText(text = track.displayedResult, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12)
                        MKText(
                            text = " (${track.displayedDiff})",
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
