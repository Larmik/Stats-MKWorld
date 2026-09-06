package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.PlayerMedallion

private val CardRadius = RoundedCornerShape(6.dp)

/**
 * Entrée de podium mutualisée (circuit / adversaire / joueur) : image + nom + couples
 * (libellé → valeur). Podiums de `StatsFullScreen` (#25/#36) et grilles Classements (#26).
 * Priorité avatar : [pictureRes] > [logo] > [initials] (+ [avatar] superposée) > `default_logo`.
 */
class PodiumEntry(
    val labelRes: Int? = null,       // circuit : @StringRes du nom de map
    val name: String? = null,        // adversaire/joueur : nom (roster > équipe)
    val pictureRes: Int? = null,     // circuit : illustration @DrawableRes
    val logo: String? = null,        // adversaire : chemin logo MKCentral (sans domaine)
    val initials: String? = null,    // joueur : initiales (fallback pastille colorée)
    val avatar: String? = null,      // joueur : chemin photo de profil MKCentral (#50 pt.4)
    val avatarColor: Color = Colors.blue, // couleur de la pastille d'initiales
    val stats: List<Pair<Int, String>> // lignes @StringRes(label) → valeur
)

/**
 * Une ligne de podium : jusqu'à [columns] `PodiumCell` à poids égal, hauteur uniforme
 * (`IntrinsicSize.Min`), complétée par des `Spacer` si moins d'entrées. [onClick] optionnel
 * (index passé à l'appelant). [contentColor] = couleur du texte.
 */
@Composable
fun ColumnScope.PodiumRow(
    entries: List<PodiumEntry>,
    columns: Int = 3,
    contentColor: Color = Colors.white,
    onClick: ((Int) -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEachIndexed { index, entry ->
            PodiumCell(entry, contentColor = contentColor, onClick = onClick?.let { { it(index) } })
        }
        repeat(columns - entries.size) { Spacer(Modifier.weight(1f)) }
    }
}

/**
 * Cellule podium : image (circuit / logo / initiales / `default_logo`), nom (2 lignes max), puis
 * lignes de stats (libellé + valeur). [contentColor] = couleur du nom et des valeurs (libellé à
 * 66 % d'alpha) ; les initiales restent blanches sur leur pastille.
 */
@Composable
fun RowScope.PodiumCell(
    entry: PodiumEntry,
    contentColor: Color = Colors.white,
    onClick: (() -> Unit)? = null
) {
    val labelColor = contentColor.copy(alpha = 0.66f)
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Colors.white30, CardRadius)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            entry.pictureRes != null -> Image(
                painter = painterResource(entry.pictureRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp))
            )
            entry.logo != null -> AsyncImage(
                model = "https://mkcentral.com${entry.logo}",
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            // Médaillon mutualisé (#50 pt.4, rule 16) : photo si dispo, initiales sinon.
            entry.initials != null -> PlayerMedallion(
                initials = entry.initials,
                avatarColor = entry.avatarColor,
                avatarPath = entry.avatar,
                size = 40.dp
            )
            else -> Image(
                painter = painterResource(R.drawable.default_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        }
        Spacer(Modifier.height(6.dp))
        MKText(
            text = entry.labelRes?.let { stringResource(it) } ?: entry.name ?: "-",
            font = Fonts.NunitoBD,
            textColor = contentColor,
            fontSize = 11,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        entry.stats.forEach { (labelRes, value) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MKText(text = stringResource(labelRes), textColor = labelColor, fontSize = 9, maxLines = 1)
                MKText(text = value, font = Fonts.NunitoBD, textColor = contentColor, fontSize = 10, maxLines = 1)
            }
        }
    }
}

/** Initiales (≤ 2) d'un nom, pour les pastilles d'avatar joueur. */
fun initialsOf(name: String?): String = name
    ?.trim()
    ?.split(" ", "_", "-")
    ?.filter { it.isNotBlank() }
    ?.take(2)
    ?.joinToString("") { it.first().uppercase() }
    ?.takeIf { it.isNotEmpty() }
    ?: "?"
