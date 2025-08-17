package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun MKTopBottomCell(indiv: Boolean, tops: List<Pair<String, Int>>?, bottoms:  List<Pair<String, Int>>?) {
    val padding = when (indiv) {
        true -> 45.dp
        else -> 25.dp
    }
    Row(Modifier
        .padding(bottom = 20.dp)
        .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
        .background(
            color = Colors.blackAlphaed,
            shape = RoundedCornerShape(5.dp)
        )) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(10.dp)) {
            MKText(text = "Tops", modifier = Modifier.padding(bottom = 10.dp), fontSize = 18, font = Fonts.NunitoBD, textColor = Colors.white)
            tops?.forEach {

                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = padding), horizontalArrangement = SpaceBetween, verticalAlignment = CenterVertically) {
                    when (indiv) {
                        true ->  MKText(text = it.first, font = Fonts.MKPosition, textColor = Colors.white, fontSize = 20)
                        else ->  MKText(text = it.first, textColor = Colors.white)
                    }
                    MKText(text = it.second.toString(), font = Fonts.Urbanist, fontSize = 16, textColor = Colors.white)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(10.dp) ) {
            MKText(text = "Bottoms", modifier = Modifier.padding(bottom = 10.dp), fontSize = 18, font = Fonts.NunitoBD,textColor = Colors.white)
            bottoms?.forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = padding), horizontalArrangement = SpaceBetween, verticalAlignment = CenterVertically) {
                    when (indiv) {
                        true ->  MKText(text = it.first, font = Fonts.MKPosition, textColor = Colors.white, fontSize = 20)
                        else ->  MKText(text = it.first, textColor = Colors.white)
                    }

                    MKText(text = it.second.toString(), font = Fonts.Urbanist, fontSize = 16, textColor = Colors.white)
                }
            }
        }
    }
}