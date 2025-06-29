package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun MapCell(
    modifier: Modifier = Modifier,
    map: Maps? = null,
    track: WarTrackDetails? = null,
    backgroundColor: Color = Colors.blackAlphaed,
    textColor: Color = Colors.white,
    borderColor: Color = Colors.white,
    onClick: (Maps) -> Unit,
    onTrackDetails: (WarTrackDetails) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val mapToDisplay = map ?: Maps.entries[track?.index ?: 0]
    Column(modifier.background(backgroundColor, RoundedCornerShape(5.dp)).border(2.dp, borderColor, RoundedCornerShape(5.dp)).clickable {
        keyboardController?.hide()
        track?.let(onTrackDetails) ?: run { onClick(mapToDisplay) }
                                                                                                                                        }, horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(mapToDisplay.cup), modifier = Modifier.size(25.dp), contentDescription = null)
                Image(painter = painterResource(mapToDisplay.picture), modifier = Modifier.width(90.dp).height(50.dp), contentDescription = null)
                Spacer(Modifier.height(10.dp))
                MKText(text = stringResource(mapToDisplay.label), font = Fonts.NunitoBD, textColor = textColor, maxLines = 1)
                MKText(text = mapToDisplay.name, fontSize = 10, textColor = textColor, font = Fonts.NunitoIT)
            }
            track?.let {
                Column(modifier = Modifier.padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = it.displayedResult, fontSize = 24, font = Fonts.NunitoBD, textColor = textColor)
                    MKText(text = it.displayedDiff, fontSize = 18, font = Fonts.NunitoBD, textColor = textColor)
                }
            }


    }
}

@Preview
@Composable
fun MapCellPreview() {
    MapCell(map = Maps.MBC, onClick = {}) {

    }
}