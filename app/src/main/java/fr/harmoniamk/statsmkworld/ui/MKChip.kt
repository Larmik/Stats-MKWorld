package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pilule interactive (`.chip` maquette) : actif = fond blanc/texte sombre, inactif = translucide.
 * Partagée (rule 16) entre les filtres de wars et le compteur de lignes Tab. `enabled = false`
 * grise le libellé et coupe le clic (butée min/max du compteur).
 */
@Composable
fun MKChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier
            .background(
                if (active) Colors.white else Colors.white30,
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (active) Colors.white else Colors.whiteBorderSoft,
                RoundedCornerShape(20.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        MKText(
            text = label,
            font = Fonts.NunitoBD,
            textColor = when {
                !enabled -> Colors.whiteAlphaed
                active -> Colors.black
                else -> Colors.white
            },
            fontSize = 12
        )
    }
}
