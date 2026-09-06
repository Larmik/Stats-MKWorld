package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Contenu « Top / Bot » (deux colonnes Tops | Bottoms, une ligne par seuil), mutualisé (#27/#64)
 * entre fiches Adversaire/Circuit et Stats équipe. [tops]/[bottoms] = listes (libellé → compte).
 * [usePositionFont] : police MKPosition pour les tables individuelles (positions).
 *
 * Tables équipe/adversaire (`usePositionFont` false, #64) : la ligne N=6 (redondante avec
 * « Records & séries ») et les lignes à 0 sont masquées. Tables individuelles : toutes les lignes.
 */
@Composable
fun ColumnScope.TopBottomColumns(
    tops: List<Pair<String, Int>>,
    bottoms: List<Pair<String, Int>>,
    usePositionFont: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TopBottomColumn(title = "Tops", entries = tops, accent = Colors.green, usePositionFont = usePositionFont)
        TopBottomColumn(title = "Bottoms", entries = bottoms, accent = Colors.red, usePositionFont = usePositionFont)
    }
}

/**
 * Lignes affichées d'une table Top/Bot équipe/adversaire (#64) : sans la ligne N=6 ni les lignes
 * à 0. Source unique du rendu ET du masquage de section (`hasDisplayableTopBottom`).
 */
fun List<Pair<String, Int>>.displayableTopBottomRows(): List<Pair<String, Int>> = this
    .filterNot { (label, _) -> label == "Top 6" || label == "Bot 6" }
    .filter { (_, count) -> count > 0 }

/** Vrai s'il reste au moins une ligne affichable (N=5→2, > 0) → aligne le masquage de section sur le rendu. */
fun hasDisplayableTopBottom(tops: List<Pair<String, Int>>, bottoms: List<Pair<String, Int>>): Boolean =
    tops.displayableTopBottomRows().isNotEmpty() || bottoms.displayableTopBottomRows().isNotEmpty()

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopBottomColumn(
    title: String,
    entries: List<Pair<String, Int>>,
    accent: androidx.compose.ui.graphics.Color,
    usePositionFont: Boolean
) {
    // Individuelles : toutes les lignes ; équipe/adversaire : lignes affichables (N=6 + zéros masqués).
    val displayed = when (usePositionFont) {
        true -> entries
        else -> entries.displayableTopBottomRows()
    }
    Column(Modifier.weight(1f)) {
        MKText(
            text = title.uppercase(),
            font = Fonts.NunitoBD,
            textColor = accent,
            fontSize = 11,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )
        displayed.forEach { (label, count) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (usePositionFont) {
                    true -> MKText(text = label, font = Fonts.MKPosition, textColor = Colors.white, fontSize = 16)
                    else -> MKText(text = label, textColor = Colors.white70, fontSize = 12, textAlign = TextAlign.Start)
                }
                MKText(text = count.toString(), font = Fonts.Urbanist, textColor = Colors.white, fontSize = 14)
            }
        }
    }
}
