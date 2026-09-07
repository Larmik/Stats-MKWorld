package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/** Préfixe MKCentral pour les chemins d'avatar/logo relatifs stockés en base. */
private const val MKCENTRAL_BASE = "https://mkcentral.com"

/**
 * Médaillon joueur **unique et partagé** (rule 16) : pastille colorée [initials] surmontée de la
 * photo de profil [avatarPath] si dispo. Fallback naturel : les initiales restent visibles dessous
 * tant que Coil n'a rien chargé (chargement ou échec → initiales conservées).
 *
 * [avatarPath] est un chemin RELATIF MKCentral (préfixé ici) ; une URL absolue passe telle quelle.
 */
@Composable
fun PlayerMedallion(
    initials: String,
    avatarColor: Color,
    modifier: Modifier = Modifier,
    avatarPath: String? = null,
    size: Dp = 40.dp,
    initialsFontSize: Int = 13,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Colors.transparent
) {
    val base = modifier
        .size(size)
        .clip(CircleShape)
        .background(avatarColor)
        .let { if (borderWidth > 0.dp) it.border(borderWidth, borderColor, CircleShape) else it }
    Box(base, contentAlignment = Alignment.Center) {
        MKText(text = initials, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = initialsFontSize, resizable = false)
        avatarPath?.takeIf { it.isNotBlank() }?.let { path ->
            val model = if (path.startsWith("http")) path else "$MKCENTRAL_BASE$path"
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        }
    }
}
