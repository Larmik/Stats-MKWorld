package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.countryFlag
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.R

@Composable
fun PlayerCell(
    modifier: Modifier = Modifier,
    player: PlayerEntity?,
    textColor: Color = Colors.white,
    backgroundColor: Color = Colors.blackAlphaed,
    position: Int? = null,
    shocksEnabled: Boolean = false,
    shockCount: Int? = null,
    onAddShock: (String) -> Unit = {},
    onRemoveShock: (String) -> Unit = {},
    onClick: (PlayerEntity) -> Unit
) {
    val shockCountState = remember { mutableStateOf(shockCount ?: 0) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier
            .background(backgroundColor, RoundedCornerShape(5.dp))
            .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
            .clickable {
                keyboardController?.hide()
                player?.let(onClick)
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MKText(text = player?.country?.countryFlag.orEmpty(), fontSize = 30)
            MKText(
                text = player?.name.orEmpty(),
                font = Fonts.NunitoBD,
                textColor = textColor,
                maxLines = 1
            )
        }

        position?.let {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                MKText(
                    text = position.toString(),
                    textColor = position.positionColor(),
                    fontSize = 40,
                    font = Fonts.MKPosition
                )
                when (val shocks = shockCountState.value.takeIf { it > 0 }) {
                    null -> Spacer(Modifier.size(20.dp))
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.shock),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        if (shocks > 1)
                            MKText(
                                text = "x$shocks",
                                font = Fonts.NunitoBdIt,
                                textColor = Colors.white
                            )
                    }
                }
            }
        }
        if (shocksEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(Colors.whiteAlphaed)
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                MKText(
                    text = "-",
                    textColor = Colors.white,
                    font = Fonts.Bungee,
                    fontSize = 18,
                    modifier = Modifier.clickable(onClick = {
                        player?.id?.takeIf { shockCountState.value > 0 }?.let {
                            onRemoveShock(it)
                            shockCountState.value -= 1
                        }
                    })
                )
                Image(
                    painter = painterResource(R.drawable.shock),
                    contentDescription = null,
                    modifier = Modifier
                        .height(30.dp)
                        .width(50.dp)
                        .padding(horizontal = 10.dp)
                )
                MKText(
                    text = "+",
                    textColor = Colors.white,
                    font = Fonts.Bungee,
                    fontSize = 20,
                    modifier = Modifier.clickable(onClick = {
                        player?.id?.let {
                            onAddShock(it)
                            shockCountState.value += 1
                        }
                    })
                )
            }
        }
    }
}

@Preview
@Composable
fun PlayerCellPreview() {
    PlayerCell(
        player = PlayerEntity(
            id = "18595",
            name = "Larii",
            country = "FR",
            role = 0,
            currentWar = "",
            isAlly = false
        ),
        position = 1,
        shocksEnabled = true,
        shockCount = 1,
        onClick = {},
        onAddShock = {},
        onRemoveShock = {})
}