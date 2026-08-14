package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius

/**
 * Couleur stable de la pastille d'avatar d'un joueur, dérivée de son id (palette
 * équipe). Partagée (rule 16) entre le wizard de création de war (`AddWarScreen`) et
 * l'écran d'actions de war (`CurrentWarActionsScreen`) qui affichent tous deux des
 * lignes joueur [MKListRow].
 */
fun playerAvatarColor(id: String): Color = ((id.hashCode() and 0x7fffffff) % 32 + 1).toTeamColor()

/**
 * Ligne de liste générique (`.lrow` de la maquette prototype UX) : carte sombre
 * translucide, pastille ronde (avatar MKCentral [avatarUrl] si présent, sinon
 * [initials]/[fallback] sur fond [avatarColor]), titre + slot [titleTrailing]
 * optionnel, sous-texte [subtitle], puis un slot de fin [trailing] (chevron, pastille
 * de sélection…).
 *
 * Composant **partagé unique** (rule 16) : mutualisé entre le pôle Profil
 * (`ProfileMemberRow` → chevron de navigation) et le wizard de création de war
 * (`AddWarScreen` → chevron pour l'adversaire, pastille ✓ pour les joueurs). Généralisé
 * par paramètres (avatar, trailing, taille de pastille), pas par duplication.
 *
 * Valeurs de style extraites de `docs/prototype/stats-mkworld-5poles.html` (`.lrow`
 * fond `--card`, bordure `--bord`, radius 6px, padding 10/12 ; `.lc` pastille ronde 34px
 * bordure blanche ; `.ln`/`.ls` titre 14/sous-texte 11).
 */
@Composable
fun MKListRow(
    initials: String,
    avatarColor: Color,
    name: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    subtitle: String? = null,
    avatarSize: Dp = 34.dp,
    onClick: (() -> Unit)? = null,
    titleTrailing: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .clip(StatCardRadius)
            .background(Colors.blackAlphaed, StatCardRadius)
            .border(1.dp, Colors.whiteBorder, StatCardRadius)
            .let { base -> onClick?.let { base.clickable(onClick = it) } ?: base }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier.size(avatarSize).clip(CircleShape).background(avatarColor).border(2.dp, Colors.white.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (avatarUrl) {
                null -> MKText(text = initials, font = Fonts.Urbanist, fontSize = 13, textColor = Colors.white, resizable = false)
                else -> AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.size(avatarSize).clip(CircleShape))
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MKText(text = name, font = Fonts.NunitoBD, fontSize = 14, textColor = Colors.white, textAlign = TextAlign.Start, maxLines = 1)
                titleTrailing?.invoke(this)
            }
            subtitle?.let {
                MKText(text = it, font = Fonts.Urbanist, fontSize = 11, textColor = Colors.white55, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
            }
        }
        trailing?.invoke()
    }
}

/** Chevron de fin (`.chev`) d'une [MKListRow] menant à un autre écran/étape. */
@Composable
fun MKListRowChevron() {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = Colors.white.copy(alpha = 0.45f),
        modifier = Modifier.size(18.dp)
    )
}

/**
 * Pastille de sélection (`.chk`) d'une [MKListRow] : cercle vide (bordure atténuée)
 * quand non sélectionné, cercle vert plein avec ✓ sombre quand [selected].
 */
@Composable
fun MKListRowCheck(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) Colors.green else Colors.transparent)
            .border(2.dp, if (selected) Colors.green else Colors.whiteBorderSoft, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) MKText(text = "✓", font = Fonts.NunitoBD, fontSize = 12, textColor = Colors.black, resizable = false)
    }
}
