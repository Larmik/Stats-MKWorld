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
 * Contenu « Top / Bot » (compteurs Top 5→2 et Bot 5→2 côté équipe/adversaire), au style
 * maquette (dans une [StatCard]). Deux colonnes (Tops | Bottoms), une ligne par seuil
 * (libellé → nombre). Mutualisé entre les fiches détail Adversaire/Circuit (#27) et la
 * page Stats équipe (#64) — remplace l'ancien `MKTopBottomCell` non aligné maquette.
 *
 * [tops] / [bottoms] = listes (libellé → compte), telles que fournies par
 * [fr.harmoniamk.statsmkworld.model.local.MapStats.topsTable] /
 * [fr.harmoniamk.statsmkworld.model.local.MapStats.bottomsTable] (équipe/adversaire) ou
 * les tables individuelles. [usePositionFont] permet d'utiliser la police MKPosition pour
 * les tables individuelles (positions), sinon la police par défaut.
 *
 * Deux règles d'affichage pour les tables **équipe/adversaire** (#64, `usePositionFont`
 * false) :
 * - **la ligne Top 6 / Bot 6 est retirée** (redondante avec le Top6/Bot6 de « Records &
 *   séries » / `RecordsTilesCard`) → seuls N = 5→2 sont affichés ;
 * - **une ligne à 0 est masquée** (on ne montre que les compteurs > 0).
 * Les tables individuelles (positions, `usePositionFont` true) conservent TOUTES leurs
 * lignes (aucun filtre) : chaque position doit rester visible, y compris à 0.
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
 * Lignes réellement AFFICHÉES d'une table Top/Bot équipe/adversaire (#64) : on retire la
 * ligne N=6 (redondante avec le Top6/Bot6 de « Records & séries ») et les lignes à 0.
 * Source de vérité unique du rendu ET du masquage de section (`hasDisplayableTopBottom`) :
 * une section n'est visible que si au moins une de ces lignes reste.
 */
fun List<Pair<String, Int>>.displayableTopBottomRows(): List<Pair<String, Int>> = this
    .filterNot { (label, _) -> label == "Top 6" || label == "Bot 6" }
    .filter { (_, count) -> count > 0 }

/**
 * Une carte Top/Bot équipe/adversaire ne doit être affichée que s'il reste au moins une
 * ligne affichable (N=5→2, > 0) sur l'une des deux colonnes. Aligne le masquage de
 * section sur le filtre de rendu de [TopBottomColumns] (pas de carte au titre vide).
 */
fun hasDisplayableTopBottom(tops: List<Pair<String, Int>>, bottoms: List<Pair<String, Int>>): Boolean =
    tops.displayableTopBottomRows().isNotEmpty() || bottoms.displayableTopBottomRows().isNotEmpty()

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopBottomColumn(
    title: String,
    entries: List<Pair<String, Int>>,
    accent: androidx.compose.ui.graphics.Color,
    usePositionFont: Boolean
) {
    // Tables individuelles (positions) : toutes les lignes ; équipe/adversaire : lignes
    // affichables uniquement (N=6 retirée + zéros masqués), même règle que le masquage
    // de section.
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
