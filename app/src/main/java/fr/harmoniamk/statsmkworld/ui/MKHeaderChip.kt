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
 * Pastille cliquable de header (« chip » d'app bar), composant **partagé unique** (rule 16) :
 * fond blanc translucide (`white30`), bordure douce, coins 10 dp, texte blanc.
 *
 * Utilisé par le déclencheur du sélecteur de saison ([MKSeasonDropdown]) ET par le
 * déclencheur « Voir par période » (#80) placé à sa gauche, pour un rendu **identique** dans
 * le header (retour utilisateur). Un `trailing` optionnel (ex. chevron « ▾ ») distingue le
 * dropdown ; sans lui la pastille est un simple bouton.
 *
 * ⚠️ **Écart assumé vs maquette (rules 13/15)** : le prototype ne prévoit pas ces pastilles
 * de header — style aligné au plus proche des boutons d'app bar de `BaseScreen`.
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
