package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun WarCell(modifier: Modifier = Modifier, viewModel: WarCellViewModel, onClick: (WarDetails) -> Unit) {
    val state = viewModel.state.collectAsState()
    val borderColor = when {
        state.value.diff?.startsWith("-") == true -> Colors.red
        state.value.diff?.startsWith("+") == true -> Colors.green
        else -> Colors.transparent
    }
    Column(modifier.background(Colors.whiteAlphaed, RoundedCornerShape(5.dp)).border(2.dp, borderColor, RoundedCornerShape(5.dp)).clickable { onClick(viewModel.details) }, horizontalAlignment = Alignment.CenterHorizontally) {
        MKText(text = state.value.date.orEmpty(), modifier = Modifier.padding(top = 5.dp), fontSize = 12, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model =  "https://mkcentral.com${state.value.teamHost?.logo}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(35.dp)
                        .clip(CircleShape)
                )
                MKText(text = state.value.teamHost?.name.orEmpty(), maxLines = 1, fontSize = 12)
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = state.value.score.orEmpty(), fontSize = 20, font = Fonts.NunitoBD, maxLines = 1)
                MKText(text = state.value.diff.orEmpty(), font = Fonts.NunitoBD)
                state.value.mapsWon?.let {
                    MKText(text = stringResource(R.string.maps_won, it.toString()), fontSize = 12, font = Fonts.NunitoIT, maxLines = 1, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model =  "https://mkcentral.com${state.value.teamOpponent?.logo}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(35.dp)
                        .clip(CircleShape)
                )
                MKText(text = state.value.teamOpponent?.name.orEmpty(), textColor = Colors.black, maxLines = 1, fontSize = 12)
            }
        }
    }

}