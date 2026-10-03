package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.extension.displayedMap
import fr.harmoniamk.statsmkworld.extension.displayedTag
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.StatCard

/**
 * Carte en-tête d'une course (résumé d'AddTrack, détail d'une course) : illustration + nom + tag
 * du circuit (#101), puis ligne de sous-titre libre ([subtitle] : score, diff colorisée…).
 * [maps] = circuits de la course : intermission → dernier circuit seul, sans tag.
 */
@Composable
fun TrackHeaderCard(maps: List<Maps>, subtitle: @Composable RowScope.() -> Unit) {
    val map = maps.displayedMap()
    StatCard {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            map?.let {
                Image(
                    painter = painterResource(it.picture),
                    contentDescription = null,
                    modifier = Modifier.width(64.dp).height(44.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Column(Modifier.weight(1f)) {
                map?.let {
                    MKText(text = stringResource(it.label), font = Fonts.Bungee, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start, maxLines = 2)
                }
                maps.displayedTag()?.let {
                    MKText(text = it, font = Fonts.NunitoIT, textColor = Colors.white, fontSize = 11, textAlign = TextAlign.Start, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp), content = subtitle)
            }
        }
    }
}
