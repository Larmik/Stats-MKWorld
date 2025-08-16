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
import androidx.compose.runtime.mutableIntStateOf
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
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarStats
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem

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
    onClick: (PlayerEntity) -> Unit,
    playerRanking: RankingItem.PlayerRanking? = null
) {
    val shockCountState = remember { mutableIntStateOf(shockCount ?: 0) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val finalPlayer = player ?: playerRanking?.player

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
            MKText(text = finalPlayer?.country?.countryFlag.orEmpty(), fontSize = 30)
            MKText(
                text = finalPlayer?.name.orEmpty(),
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
                when (val shocks = shockCountState.intValue.takeIf { it > 0 }) {
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
                        player?.id?.takeIf { shockCountState.intValue > 0 }?.let {
                            onRemoveShock(it)
                            shockCountState.intValue -= 1
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
                            shockCountState.intValue += 1
                        }
                    })
                )
            }
        }
        playerRanking?.let {
            Row(Modifier.padding(bottom = 10.dp)) {
                Column {
                    MKText(text = "Wars jouées", fontSize = 12, textColor = Colors.white)
                    MKText(text ="Winrate", fontSize = 12, textColor = Colors.white)
                    MKText(text = "Score moyen", fontSize = 12, textColor = Colors.white)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    MKText(text = it.warsPlayedLabel, font = Fonts.NunitoBdIt, fontSize = 12, textColor = Colors.white)
                    MKText(text = it.winrateLabel, font = Fonts.NunitoBdIt, fontSize = 12, textColor = Colors.white)
                    MKText(text = it.averageLabel, font = Fonts.NunitoBdIt, fontSize = 12, textColor = Colors.white)
                }
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
        position = null,
        shocksEnabled = false,
        shockCount = null,
        onClick = {},
        onAddShock = {},
        onRemoveShock = {},
        playerRanking = RankingItem.PlayerRanking(
            player =  PlayerEntity(
                id = "18595",
                name = "Larii",
                country = "FR",
                role = 0,
                currentWar = "",
                isAlly = false
            ),
            stats = Stats(
                WarStats(listOf()),
                null,
                null,
                null,
                listOf(),
                listOf(),
                listOf(),
            )
        )
    )
}