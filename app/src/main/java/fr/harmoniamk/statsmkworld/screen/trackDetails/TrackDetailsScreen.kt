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
 * Relecture en lecture seule d'une course jouée (#47) : carte en-tête ([TrackHeaderCard]),
 * grille « Positions & shocks », et bouton « Éditer la course » (visible tant que la war n'est
 * pas validée, cf. [TrackDetailsViewModel]). Graphe racine → pas de bottombar (rule 17).
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
 * Tuile joueur (lecture seule) : nom + position (font `MKPosition`, colorée par [Int.positionColor])
 * + icône shock `x{n}` si [shockCount] > 0.
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
 * Carte en-tête : illustration du dernier circuit + nom + sous-titre « Course N · score (±diff) »,
 * diff colorisée ([Int.diffColor]). En 24p, pas de score/diff par manche.
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
                // Sous-titre « Course N · score (±diff) », diff colorisée (12p uniquement).
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
