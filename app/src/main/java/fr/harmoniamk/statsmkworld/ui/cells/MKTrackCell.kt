package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/** Rayon de coin des cartes translucides (aligné sur `CurrentWar`/maquette). */
private val TrackCellRadius = RoundedCornerShape(6.dp)

/**
 * Cellule de course/circuit partagée (rule 16). Horizontal : bande colorée (accent) · image + nom ·
 * zone shocks réservée · score + diff. Deux modes :
 * - **course jouée** (`track != null`) : score + diff colorisée, accent selon la diff. → détail.
 * - **sélection** (`map != null`, sans `track`) : image + nom seuls, accent blanc. → sélection.
 *
 * Hauteur uniforme (84 dp) calée sur « nom sur 2 lignes » → cellules alignées.
 */
@Composable
fun MKTrackCell(
    modifier: Modifier = Modifier,
    track: WarTrackDetails? = null,
    map: Maps? = null,
    is24p: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    // Course jouée : dernier segment (arrivée) d'une éventuelle intermission ; sinon le circuit fourni.
    val displayedMap = map ?: track?.index?.lastOrNull()?.toInt()?.let { Maps.entries.getOrNull(it) }
    // Accent (liseré + diff) : vert manche gagnée, rouge perdue (blanc = nul / 24 j / sélection).
    val accent: Color = when {
        track == null -> if (selected) Colors.green else Colors.white
        is24p -> Colors.white
        track.displayedDiff.startsWith("+") -> Colors.green
        track.displayedDiff.startsWith("-") -> Colors.red
        else -> Colors.white
    }
    val shockCount = track?.track?.shocks.orEmpty().sumOf { it.count }
    Row(
        modifier
            .height(84.dp)
            .clip(TrackCellRadius)
            .background(Colors.white30, TrackCellRadius)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Bande colorée verticale (bord gauche, pleine hauteur).
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        // 2. Colonne centrale : image du circuit + nom (2 lignes réservées → hauteur égale).
        Column(
            Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            displayedMap?.let {
                Image(
                    painter = painterResource(it.picture),
                    contentDescription = null,
                    modifier = Modifier.size(width = 56.dp, height = 36.dp).clip(RoundedCornerShape(4.dp))
                )
            }
            Box(Modifier.height(32.dp).padding(top = 4.dp), contentAlignment = Alignment.TopCenter) {
                MKText(
                    text = displayedMap?.label?.let { stringResource(it) } ?: "-",
                    font = Fonts.NunitoBD,
                    textColor = Colors.white,
                    fontSize = 12,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
        // 3. Zone shocks : largeur TOUJOURS réservée (placeholder invisible si aucun shock).
        Column(
            Modifier.width(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(shockCount) {
                Image(
                    painter = painterResource(R.drawable.shock),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        // 4. Score + diff (course jouée uniquement), à DROITE, centrés verticalement.
        track?.let {
            Column(
                Modifier.padding(start = 6.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(
                    text = if (is24p) it.teamScore.toString() else it.displayedResult.replace(" - ", "-"),
                    font = Fonts.Urbanist,
                    textColor = Colors.white,
                    fontSize = 15,
                    maxLines = 1
                )
                if (!is24p) MKText(
                    text = it.displayedDiff,
                    font = Fonts.Urbanist,
                    textColor = accent,
                    fontSize = 12,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
