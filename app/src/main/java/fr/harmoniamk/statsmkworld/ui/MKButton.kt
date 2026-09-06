package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ButtonRadius = RoundedCornerShape(10.dp)

/**
 * Bouton **unique** de l'app (#50, rule 16) : fond blanc translucide (`Colors.white30`), sans
 * bordure, libellé + icône majuscules (Urbanist), coins 10 dp. Tous les boutons passent par ici.
 *
 * [icon] optionnel : icône de tête (métriques de l'ancien `WarActionButton` : hauteur 46 dp).
 * [textColor] ajuste le contraste du libellé selon le fond (blanc sur sombre, `Colors.black` sur
 * surface claire type `MKDialog`), pas une variante de style. Désactivé = fond + texte atténués.
 */
@Composable
fun MKButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: Int? = null,
    textColor: Color = Colors.white,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Désactivé : atténuer la couleur demandée plutôt qu'une couleur fixe, lisible partout.
    val resolvedTextColor = if (enabled) textColor else textColor.copy(alpha = 0.4f)
    val backgroundColor = if (enabled) Colors.white30 else Colors.whiteAlphaed

    Button(
        modifier = modifier,
        onClick = onClick,
        // Aucune élévation : le fond est porté par le Row interne, pas par le container Material.
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, disabledElevation = 0.dp),
        contentPadding = PaddingValues(),
        enabled = enabled,
        // Container Material toujours transparent : sinon le disabledContainerColor par défaut
        // (gris onSurface .12) réafficherait une boîte derrière notre Row désactivé (#50).
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.Transparent,
            disabledContentColor = Color.Transparent
        ),
        shape = ButtonRadius,
    ) {
        Row(
            modifier = Modifier
                .background(color = backgroundColor, shape = ButtonRadius)
                .clip(ButtonRadius)
                // Avec icône : hauteur 46 dp / padding 12 dp (rendu ancien WarActionButton).
                .let { if (icon != null) it.height(46.dp) else it }
                .padding(horizontal = if (icon != null) 12.dp else 16.dp, vertical = if (icon != null) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
            }
            Box(contentAlignment = Alignment.Center) {
                MKText(
                    text = text.uppercase(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clearAndSetSemantics { contentDescription = text }
                        .let { if (icon == null) it.padding(horizontal = 16.dp) else it },
                    font = Fonts.Urbanist,
                    fontSize = if (icon != null) 12 else 14,
                    maxLines = 1,
                    textColor = resolvedTextColor,
                )
            }
        }
    }
}
