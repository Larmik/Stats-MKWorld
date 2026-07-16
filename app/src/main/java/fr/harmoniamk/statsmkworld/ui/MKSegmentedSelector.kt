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
 * Segmenté standard de l'app (style « pill » de la maquette prototype UX) : conteneur
 * arrondi translucide, item **actif** = fond blanc plein / texte sombre, item
 * **inactif** = texte contrasté sur fond translucide. C'est LE composant segmented du
 * projet — ne pas recréer de segmented local (cf. rule 15).
 *
 * Composant **stateless** : la sélection est pilotée par [page] (index sélectionné) ;
 * [onClick] remonte le nouvel index à l'appelant, qui détient l'état.
 *
 * @param onDark `true` quand le segmented est posé sur une **carte sombre** (ex.
 *   dashboard Accueil, cartes `blackAlphaed`) : conteneur blanc très translucide,
 *   texte inactif **blanc**. Par défaut (`false`), le segmented est posé sur le
 *   **fond clair du dégradé de `BaseScreen`** : conteneur sombre translucide, texte
 *   inactif **sombre** — lisible sur le dégradé coloré. L'item actif (pastille blanche
 *   + texte sombre) reste identique dans les deux cas (contraste sur les deux fonds).
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
