package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.BorderStroke
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
 * Bouton **unique** de l'app (#50), aligné sur le style `.btn2` de la maquette — celui des
 * boutons de référence « Générer le Tab » / « Voir l'adversaire » : fond blanc translucide
 * (`Colors.white30`), bordure douce (`Colors.whiteBorderSoft`), libellé majuscule (Urbanist),
 * coins 10 dp. **Tous** les boutons de l'app passent par ici (l'ancien `WarActionButton` a
 * été fusionné ; l'ancienne variante `Gradient`/`.cta` supprimée — rule 16).
 *
 * [icon] optionnel : drawable d'icône **de tête** (16 dp), pour les boutons à icône (ex.
 * partage / adversaire des détails de war). Sans icône, le libellé est centré (fontSize 14) ;
 * avec icône, le rendu reprend exactement l'ancien `WarActionButton` (icône 16 dp + espace
 * 8 dp + libellé fontSize 12).
 *
 * [textColor] n'est PAS une variante de style : juste l'ajustement de contraste du libellé
 * selon le fond (blanc par défaut sur le dégradé/cartes sombres ; `Colors.black` sur une
 * surface claire type dialog). L'état désactivé atténue fond + texte.
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
    val resolvedTextColor = if (enabled) textColor else Colors.blackAlphaed
    val backgroundColor = if (enabled) Colors.white30 else Colors.whiteAlphaed
    val borderColor = if (enabled) Colors.whiteBorderSoft else Colors.transparent

    Button(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(1.dp, borderColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = ButtonRadius,
    ) {
        Row(
            modifier = Modifier
                .background(color = backgroundColor, shape = ButtonRadius)
                .clip(ButtonRadius)
                // Bouton à icône : hauteur fixe 46 dp + padding horizontal 12 dp (rendu exact
                // de l'ancien WarActionButton). Sans icône : padding vertical 8 dp / horizontal 16 dp.
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
