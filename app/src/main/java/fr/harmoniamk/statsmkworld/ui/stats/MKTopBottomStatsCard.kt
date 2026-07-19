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
 * Contenu « Top / Bot » (compteurs Top 2→6 et Bot 2→6), au style maquette (dans une
 * [StatCard]). Deux colonnes (Tops | Bottoms), une ligne par seuil (libellé → nombre).
 * Mutualisé entre les fiches détail Adversaire/Circuit (#27) — remplace l'ancien
 * `MKTopBottomCell` non aligné maquette pour ces écrans.
 *
 * [tops] / [bottoms] = listes (libellé → compte), telles que fournies par
 * [fr.harmoniamk.statsmkworld.model.local.MapStats.topsTable] /
 * [fr.harmoniamk.statsmkworld.model.local.MapStats.bottomsTable] (équipe) ou les tables
 * individuelles. [labelFont] permet d'utiliser la police MKPosition pour les tables
 * individuelles (positions), sinon la police par défaut.
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

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopBottomColumn(
    title: String,
    entries: List<Pair<String, Int>>,
    accent: androidx.compose.ui.graphics.Color,
    usePositionFont: Boolean
) {
    Column(Modifier.weight(1f)) {
        MKText(
            text = title.uppercase(),
            font = Fonts.NunitoBD,
            textColor = accent,
            fontSize = 11,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )
        entries.forEach { (label, count) ->
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
