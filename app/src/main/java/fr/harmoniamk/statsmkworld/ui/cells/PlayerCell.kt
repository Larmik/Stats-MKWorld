package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.countryFlag
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun PlayerCell(modifier: Modifier = Modifier, player: PlayerEntity?, textColor: Color = Colors.white, backgroundColor: Color = Colors.blackAlphaed, position: Int? = null, onClick: (PlayerEntity) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier.background(backgroundColor, RoundedCornerShape(5.dp)).border(1.dp, Colors.white, RoundedCornerShape(5.dp)).clickable {
        keyboardController?.hide()

        player?.let(onClick)
                                                                                                                                         }, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MKText(text = player?.country?.countryFlag.orEmpty(), fontSize = 30)
            MKText(text = player?.name.orEmpty(), font = Fonts.NunitoBD, textColor = textColor, maxLines = 1)
        }
        position?.let {
            MKText(text = position.toString(), textColor = position.positionColor(), fontSize = 40, font = Fonts.MKPosition, modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}

@Preview
@Composable
fun PlayerCellPreview() {
    PlayerCell(player = PlayerEntity(id = "18595", name = "Larii", country = "FR", role = 0, currentWar = "", isAlly = false), position = 1){}
}