package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun WarPlayersCell(modifier: Modifier = Modifier, players: List<PlayerScore>, trackCount: Int) {
    val splitIndex = when (players.size % 2) {
        0 -> players.size / 2
        else -> players.size / 2 + 1
    }
    Column(
        modifier
            .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
            .border(1.dp, Colors.white, RoundedCornerShape(5.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.padding(10.dp)) {
            LazyColumn(Modifier.weight(1f)) {
                items(items = players.safeSubList(0, splitIndex)) {
                    Row(
                        Modifier.padding(vertical = 1.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.width(120.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val text = when (it.trackPlayed in 1 until trackCount) {
                                true -> "${it.player?.name.orEmpty()} (${it.trackPlayed})"
                                else -> it.player?.name.orEmpty()
                            }
                            MKText(
                                modifier = Modifier.padding(horizontal = 5.dp),
                                text = text,
                                maxLines = 1,
                                textColor = Colors.white,
                                resizable = false
                            )
                        }
                        MKText(
                            text = it.score.toString(),
                            font = Fonts.NunitoBD,
                            textColor = Colors.white
                        )
                        it.shockCount.takeIf { it > 0 }?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(painter = painterResource(id = R.drawable.shock), contentDescription = null, modifier = Modifier.size(15.dp))
                                if (it > 1) MKText(text ="x$it", fontSize = 12, textColor = Colors.white)
                            }
                        }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(players.safeSubList(splitIndex, players.size)) {
                    Row(
                        Modifier.padding(vertical = 1.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.width(120.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val text = when (it.trackPlayed in 1 until trackCount) {
                                true -> "${it.player?.name.orEmpty()} (${it.trackPlayed})"
                                else -> it.player?.name.orEmpty()
                            }
                            MKText(
                                modifier = Modifier.padding(horizontal = 5.dp),
                                text = text,
                                maxLines = 1,
                                textColor = Colors.white,
                                resizable = false
                            )

                        }
                        MKText(
                            text = it.score.toString(),
                            font = Fonts.NunitoBD,
                            textColor = Colors.white
                        )
                        it.shockCount.takeIf { it > 0 }?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(painter = painterResource(id = R.drawable.shock), contentDescription = null, modifier = Modifier.size(15.dp))
                                if (it > 1) MKText(text ="x$it", fontSize = 12, textColor = Colors.white)
                            }
                        }
                    }
                }
            }
        }
    }
}