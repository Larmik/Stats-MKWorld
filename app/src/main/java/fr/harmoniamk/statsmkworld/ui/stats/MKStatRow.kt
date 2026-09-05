package fr.harmoniamk.statsmkworld.ui.stats

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
 * Ligne « libellé … valeur » réutilisée par les sections de stats
 * (`MKRecordsCell`, `MKAdvancedStatsCell`).
 *
 * Si [info] est fourni, un bouton d'information rond ([MKStatInfoButton]) est
 * affiché après le libellé : au clic, il ouvre un dialog expliquant l'indicateur
 * (titre = [label], message = [info]).
 */
@Composable
fun MKStatRow(label: String, value: String, info: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MKText(
            text = label,
            textColor = Colors.white,
            fontSize = 14
        )
        info?.let {
            MKStatInfoButton(
                title = label,
                message = it,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        MKText(
            text = value,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 14,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            textAlign = TextAlign.End
        )
    }
}
