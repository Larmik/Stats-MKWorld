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
 * Bouton **unique** de l'app (#50) : bouton **PLEIN** (solide, **sans bordure**), fond
 * sombre opaque `Colors.black` (= `--ink` #3C4043 de la maquette), libellé (et icône)
 * **blancs** en majuscules (Urbanist), coins 10 dp. Le fond sombre garde les libellés ET
 * les icônes blanches (`ic_share`/`ic_cup`) lisibles sur **tous** les fonds où le bouton
 * apparaît — cartes `blackAlphaed`, dégradé de `BaseScreen`, et surface claire de
 * `MKDialog`. **Tous** les boutons passent par ici (l'ancien `WarActionButton` fusionné ;
 * les variantes `Gradient`/`.cta` et `.btn2`/bordé supprimées — rule 16).
 *
 * [icon] optionnel : drawable d'icône **de tête** (16 dp, blanche). Sans icône, le libellé
 * est centré (fontSize 14) ; avec icône, le rendu reprend les métriques de l'ancien
 * `WarActionButton` (hauteur 46 dp, icône 16 dp + espace 8 dp + libellé fontSize 12).
 *
 * [textColor] n'est PAS une variante de style : juste l'ajustement de contraste du libellé
 * (blanc par défaut sur le fond sombre plein). L'état désactivé atténue fond + texte.
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
    val resolvedTextColor = if (enabled) textColor else Colors.white55
    // Bouton PLEIN sans bordure : fond sombre opaque (enabled) / atténué (disabled).
    val backgroundColor = if (enabled) Colors.black else Colors.blackAlphaed

    Button(
        modifier = modifier,
        onClick = onClick,
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
