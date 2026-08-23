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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Styles de bouton alignés sur la maquette prototype UX (#50 pt.1) :
 * - [Gradient] = bouton d'action principal `.cta` : dégradé violet→bleu→vert,
 *   texte sombre en capitales (Urbanist), coins arrondis 10 dp, légère ombre.
 * - [Minor] = bouton secondaire `.btn2` : fond blanc translucide, bordure douce,
 *   texte de la couleur [color] (blanc sur carte/fond sombre, sombre sur dialog clair).
 */
sealed interface MKButtonStyle {
    data class Minor(val color: Color) : MKButtonStyle
    data object Gradient : MKButtonStyle
}

private val ButtonRadius = RoundedCornerShape(10.dp)

// Dégradé exact du `.cta` de la maquette (linear-gradient(90deg,#D7AEFB,#AECBFA 45%,#81C995)).
// Valeurs pastel figées ici (indépendantes de Colors.green/red, assombris pour le contraste
// du texte) pour rester fidèle au rendu du CTA.
private val CtaGradient = listOf(Color(0xFFD7AEFB), Color(0xFFAECBFA), Color(0xFF81C995))

@Composable
fun MKButton(
    modifier: Modifier = Modifier,
    style: MKButtonStyle,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor: Color
    val backgroundGradient: List<Color>
    val textColor: Color
    val borderColor: Color
    val elevation: Dp
    when {
        style is MKButtonStyle.Gradient && enabled -> {
            // `.cta` : dégradé, texte sombre, pas de bordure, légère ombre.
            textColor = Colors.black
            backgroundColor = Colors.transparent
            backgroundGradient = CtaGradient
            borderColor = Colors.transparent
            elevation = 4.dp
        }

        style is MKButtonStyle.Minor && enabled -> {
            // `.btn2` : fond blanc translucide, bordure douce, texte teinté.
            textColor = style.color
            backgroundColor = Colors.white30
            backgroundGradient = listOf()
            borderColor = Colors.whiteBorderSoft
            elevation = 0.dp
        }

        // Désactivé (Gradient comme Minor) : gris translucide, texte atténué.
        else -> {
            textColor = Colors.blackAlphaed
            backgroundColor = Colors.whiteAlphaed
            backgroundGradient = listOf()
            borderColor = Colors.transparent
            elevation = 0.dp
        }
    }

    Button(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(1.dp, borderColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation),
        contentPadding = PaddingValues(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = ButtonRadius,
    ) {

        val backgroundModifier = when (backgroundGradient.isNotEmpty()) {
            true -> Modifier.background(brush = Brush.horizontalGradient(colors = backgroundGradient), shape = ButtonRadius)
            else -> Modifier.background(color = backgroundColor, shape = ButtonRadius)
        }

        Box(
            modifier = backgroundModifier
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
                textColor = textColor,
            )
        }
    }

}
