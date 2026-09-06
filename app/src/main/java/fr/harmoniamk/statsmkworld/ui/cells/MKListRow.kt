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
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius

/** Couleur stable de la pastille d'un joueur, dérivée de son id (palette équipe). */
fun playerAvatarColor(id: String): Color = ((id.hashCode() and 0x7fffffff) % 32 + 1).toTeamColor()

/**
 * Ligne de liste générique (`.lrow` maquette) : carte sombre, pastille ronde (avatar [avatarUrl]
 * sinon [initials] sur [avatarColor]), titre + [titleTrailing], [subtitle], slot [trailing].
 *
 * Composant **partagé unique** (rule 16) entre le pôle Profil et le wizard AddWar, généralisé
 * par paramètres.
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
        // Médaillon joueur mutualisé (rule 16) : photo si dispo, initiales sinon.
        PlayerMedallion(
            initials = initials,
            avatarColor = avatarColor,
            avatarPath = avatarUrl,
            size = avatarSize,
            borderWidth = 2.dp,
            borderColor = Colors.white.copy(alpha = 0.75f)
        )
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
