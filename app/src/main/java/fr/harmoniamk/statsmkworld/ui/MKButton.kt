package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ButtonRadius = RoundedCornerShape(10.dp)

/**
 * Bouton unique de l'app (#50), aligné sur le style `.btn2` de la maquette — celui des
 * boutons de référence « Générer le Tab » / « Voir l'adversaire » (`WarActionButton`) :
 * fond blanc translucide (`Colors.white30`), bordure douce (`Colors.whiteBorderSoft`),
 * libellé majuscule (Urbanist). **Tous** les `MKButton` partagent ce style (demande
 * utilisateur : harmonie ; l'ancienne variante `Gradient`/`.cta` a été supprimée).
 *
 * [textColor] n'est PAS une variante de style : juste l'ajustement de contraste du
 * libellé selon le fond (blanc par défaut sur le dégradé/cartes sombres ; `Colors.black`
 * sur une surface claire type dialog). L'état désactivé atténue fond + texte.
 */
@Composable
fun MKButton(
    modifier: Modifier = Modifier,
    text: String,
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
        Box(
            modifier = Modifier
                .background(color = backgroundColor, shape = ButtonRadius)
                .clip(ButtonRadius)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            MKText(
                text = text.uppercase(),
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { contentDescription = text }.padding(horizontal = 16.dp),
                font = Fonts.Urbanist,
                fontSize = 14,
                maxLines = 1,
                textColor = resolvedTextColor,
            )
        }
    }
}
