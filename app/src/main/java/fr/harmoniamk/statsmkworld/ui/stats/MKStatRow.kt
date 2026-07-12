package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Ligne « libellé … valeur » réutilisée par les sections de stats
 * (`MKRecordsCell`, `MKAdvancedStatsCell`).
 */
@Composable
fun MKStatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MKText(
            text = label,
            textColor = Colors.white,
            fontSize = 14,
            modifier = Modifier.weight(1f)
        )
        MKText(
            text = value,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 14
        )
    }
}
