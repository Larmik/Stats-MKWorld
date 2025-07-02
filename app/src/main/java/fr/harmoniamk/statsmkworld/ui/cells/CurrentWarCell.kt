package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun CurrentWarCell(modifier: Modifier = Modifier, viewModel: CurrentWarCellViewModel = hiltViewModel(), onClick: () -> Unit) {
    val state = viewModel.state.collectAsState()
    Column(modifier.background(Colors.blackAlphaed, RoundedCornerShape(5.dp)).border(1.dp, Colors.white, RoundedCornerShape(5.dp)).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model =  "https://mkcentral.com${state.value.teamHost?.logo}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                )
                MKText(text = state.value.teamHost?.name.orEmpty(), textColor = Colors.white, fontSize = 16, font = Fonts.NunitoRG, maxLines = 1)
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = state.value.score.orEmpty(), textColor = Colors.white, fontSize = 28, font = Fonts.NunitoBD, maxLines = 1)
                MKText(text = state.value.diff.orEmpty(), textColor = Colors.white, fontSize = 20, font = Fonts.NunitoBD, maxLines = 1)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model =  "https://mkcentral.com${state.value.teamOpponent?.logo}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                )
                MKText(text = state.value.teamOpponent?.name.orEmpty(), textColor = Colors.white, fontSize = 16, font = Fonts.NunitoRG, maxLines = 1)
            }

        }
        MKText(text = stringResource(R.string.remaining_courses, state.value.remaining.toString()), textColor = Colors.white, modifier = Modifier.padding(bottom = 5.dp))
    }
}