package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun PositionCell(position: Int, modifier: Modifier = Modifier, isVisible: Boolean = true, onClick: (Int) -> Unit) {
    Column(modifier.background(if (isVisible) Colors.blackAlphaed else Colors.transparent, RoundedCornerShape(5.dp)).border(1.dp, if (isVisible) Colors.white else Colors.transparent, RoundedCornerShape(5.dp)).clickable { if (isVisible) onClick(position) }.alpha(if (isVisible) 1f else 0f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MKText(text = position.toString(), textColor = position.positionColor(), fontSize = 70, font = Fonts.MKPosition)
    }
}