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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface MKButtonStyle {
    data class Standard(val color: Color) : MKButtonStyle
    data class Minor(val color: Color) : MKButtonStyle
    data object Gradient: MKButtonStyle
}


@Composable
fun MKButton(
    modifier: Modifier = Modifier,
    style: MKButtonStyle,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var backgroundColor: Color = Colors.transparent
    var bachgroundGradient: List<Color> = listOf()
    val textColor: Color
    val borderColor: Color
    val elevation: Dp
    when {
        // Standard
        style is MKButtonStyle.Standard && enabled -> {
            textColor = Colors.black
            backgroundColor = style.color
            borderColor = Colors.transparent
            elevation = 5.dp
        }
        // Mineur
        style is MKButtonStyle.Minor && enabled -> {
            textColor = style.color
            borderColor = style.color
            elevation = 0.dp
        }

        style is MKButtonStyle.Gradient && enabled -> {
            textColor = Colors.black
            bachgroundGradient = listOf(Colors.purple, Colors.blue, Colors.blue, Colors.green)
            borderColor = Colors.blue
            elevation = 5.dp
        }

        // By default, Standard Disable
        else -> {
            textColor = Colors.blackAlphaed
            backgroundColor = Colors.whiteAlphaed
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
        shape = RoundedCornerShape(10.dp),
    ) {

        val backgroundModifier = when (bachgroundGradient.isNotEmpty()) {
            true -> Modifier.background(brush = Brush.horizontalGradient(colors = bachgroundGradient), shape = RoundedCornerShape(10.dp))
            else -> Modifier.background(color = backgroundColor, shape = RoundedCornerShape(10.dp))
        }

        Box(
            modifier = backgroundModifier
                .clip(RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            MKText(
                text = text.uppercase(),
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { }.padding(horizontal = 16.dp),
                font = Fonts.Urbanist,
                fontSize = 14,
                maxLines = 1,
                textColor = textColor,
            )
        }
    }

}

