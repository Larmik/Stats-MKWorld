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
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.cells.MKListRowCheck

/**
 * Case à cocher libellée (#103) : pilule translucide (style [MKChip] inactif) + pastille
 * [MKListRowCheck] réutilisée. Stateless : [checked] piloté par l'appelant, clic remonté via [onClick].
 */
@Composable
fun MKCheckbox(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .clip(shape)
            .background(Colors.white30, shape)
            .border(1.dp, Colors.whiteBorderSoft, shape)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        MKListRowCheck(selected = checked)
        MKText(text = label, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12, maxLines = 1)
    }
}
