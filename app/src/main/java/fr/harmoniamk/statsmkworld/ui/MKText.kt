package fr.harmoniamk.statsmkworld.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

@Composable
fun MKText(modifier: Modifier = Modifier, text: Any, font: Font = Fonts.NunitoRG, fontSize: Int = 14, textColor: Color = Colors.black, maxLines: Int = Integer.MAX_VALUE, textAlign: TextAlign = TextAlign.Center) {
    val targetTextSizeHeight = TextUnit(fontSize.toFloat(), TextUnitType.Sp)
    val textSize = remember { mutableStateOf(targetTextSizeHeight) }

    Text(
        text = when (text) {
            is Int -> stringResource(id = text)
            else -> text.toString()
        },
        fontFamily = FontFamily(font),
        modifier = modifier,
        textAlign = textAlign,
        fontSize = textSize.value,
        color = textColor,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        onTextLayout = {
            val maxCurrentLineIndex = it.lineCount - 1
            if (it.isLineEllipsized(maxCurrentLineIndex))
                textSize.value = textSize.value.times(0.9f)
        }
    )
}