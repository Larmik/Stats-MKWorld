package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty

sealed interface WarScoreStyle {
    data object Normal : WarScoreStyle
    data object Small : WarScoreStyle
}

@Composable
fun WarScoreView(
    modifier: Modifier = Modifier,
    style: WarScoreStyle = WarScoreStyle.Normal,
    teamHost: TeamEntity?,
    teamOpponent: TeamEntity?,
    score: String,
    diff: String,
    penalties: List<WarPenalty>,
    shockCount: Int = 0
) {

    val teamNameSize: Int
    val logoSize: Dp
    val scoreSize: Int
    val diffSize: Int

    when {
        style is WarScoreStyle.Normal -> {
            teamNameSize = 20
            logoSize = 50.dp
            scoreSize = 32
            diffSize = 24
        }

        else -> {
            teamNameSize = 14
            logoSize = 30.dp
            scoreSize = 24
            diffSize = 18
        }
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = "https://mkcentral.com${teamHost?.logo}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                )
                MKText(text = teamHost?.name.orEmpty(), fontSize = teamNameSize, maxLines = 1)
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(text = score, fontSize = scoreSize, font = Fonts.NunitoBD, maxLines = 1)
                MKText(text = diff, fontSize = diffSize, font = Fonts.NunitoBD)
                shockCount.takeIf { it > 0  }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                        Image(
                            painter = painterResource(R.drawable.shock),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        MKText(
                            text = "x$it",
                            font = Fonts.NunitoBdIt,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = "https://mkcentral.com${teamOpponent?.logo}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                )
                MKText(
                    text = teamOpponent?.name.orEmpty(),
                    textColor = Colors.black,
                    fontSize = teamNameSize,
                    maxLines = 1
                )
            }
        }
        penalties.takeIf { it.isNotEmpty() }?.let {
            Row(Modifier
                .wrapContentHeight()
                .padding(horizontal = 40.dp)) {
                penalties.filter { it.teamId == teamHost?.id }.takeIf { it.isNotEmpty() }?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = "Pénalité")
                        MKText(text = "-${it.sumOf { it.amount }}")
                    }
                }
                penalties.filter { it.teamId == teamOpponent?.id }.takeIf { it.isNotEmpty() }?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = "Pénalité")
                        MKText(text = "-${it.sumOf { it.amount }}")
                    }
                }
            }
        }
    }

}