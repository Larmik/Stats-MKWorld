package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Segmenté standard de l'app (« pill » maquette) : item actif = fond blanc/texte sombre, inactif
 * = texte contrasté sur fond translucide. LE composant segmented du projet, ne pas recréer (rule 15).
 * Stateless : sélection pilotée par [page], nouvel index remonté via [onClick].
 *
 * @param onDark `true` sur carte sombre (dashboard Accueil) → texte inactif blanc ; `false`
 *   (défaut) sur le dégradé clair de `BaseScreen` → texte inactif sombre. Item actif identique.
 */
@Composable
fun MKSegmentedSelector(
    items: List<String>,
    page: Int = 0,
    onDark: Boolean = false,
    onClick: (Int) -> Unit
) {
    val containerColor = if (onDark) Colors.white30 else Colors.blackAlphaed
    val inactiveTextColor = if (onDark) Colors.white else Colors.black
    Row(
        Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.whiteBorderSoft, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items.forEachIndexed { index, label ->
            val active = index == page
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Colors.white else Colors.transparent)
                    .clickable { onClick(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                MKText(
                    text = label,
                    font = Fonts.NunitoBD,
                    textColor = if (active) Colors.black else inactiveTextColor,
                    fontSize = 13,
                    maxLines = 1
                )
            }
        }
    }
}
