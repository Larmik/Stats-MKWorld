package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pastille cliquable de header, composant **partagé unique** (rule 16) : fond blanc translucide,
 * coins 10 dp. Utilisée par le dropdown de saison ([MKSeasonDropdown]) et « Voir par période »
 * (#80) pour un rendu identique ; `trailing` optionnel (chevron) distingue le dropdown.
 *
 * ⚠️ Écart assumé vs maquette (rules 13/15) : le prototype ne prévoit pas ces pastilles de header.
 */
@Composable
fun MKHeaderChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Colors.white30, shape)
            .border(1.dp, Colors.whiteBorderSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MKText(
            text = label,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 12,
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        trailing?.invoke()
    }
}
