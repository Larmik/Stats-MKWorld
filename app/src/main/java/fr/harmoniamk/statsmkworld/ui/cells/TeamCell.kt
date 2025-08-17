package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText


@Composable
fun TeamCell(
    modifier: Modifier = Modifier,
    team: TeamEntity?,
    teamRanking: RankingItem.OpponentRanking? = null,
    teamStatsLabel: String? = null,
    userId: String? = null,
    onClick: (TeamEntity) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val finalTeam = team ?: teamRanking?.team

    Column(
        modifier
            .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
            .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
            .clickable {
                keyboardController?.hide()
                finalTeam?.let(onClick)
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "https://mkcentral.com${finalTeam?.logo}",
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(10.dp))
            MKText(
                text = finalTeam?.name.orEmpty(),
                font = Fonts.NunitoBD,
                textColor = Colors.white,
                maxLines = 1
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 70.dp)
                    .background(
                        color = finalTeam?.color.toTeamColor(),
                        shape = RoundedCornerShape(5.dp)
                    )
            )
            {
                MKText(
                    text = finalTeam?.tag.orEmpty(),
                    fontSize = 16,
                    font = Fonts.NunitoBD,
                    textColor = Colors.white,
                    modifier = Modifier.padding(5.dp)
                )
            }

            teamRanking?.let {
                Row(Modifier.padding(vertical = 10.dp)) {
                    Column {
                        MKText(text = "Wars jouées", fontSize = 12, textColor = Colors.white)
                        MKText(text = "Winrate", fontSize = 12, textColor = Colors.white)
                        MKText(text = "Score moyen", fontSize = 12, textColor = Colors.white)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        MKText(
                            text = it.warsPlayedLabel,
                            font = Fonts.NunitoBdIt,
                            fontSize = 12,
                            textColor = Colors.white
                        )
                        MKText(
                            text = it.winrateLabel,
                            font = Fonts.NunitoBdIt,
                            fontSize = 12,
                            textColor = Colors.white
                        )
                        MKText(
                            text = when (userId) {
                                null -> it.averageLabel
                                else -> it.stats.averagePoints.toString()
                            },
                            font = Fonts.NunitoBdIt,
                            fontSize = 12,
                            textColor = Colors.white
                        )
                    }
                }
            }
            teamStatsLabel?.let {
                MKText(text = it, fontSize = 12, textColor = Colors.white, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Preview
@Composable
fun TeamCellPreview() {
    TeamCell(
        team = TeamEntity(
            id = "874",
            name = "Harmonia",
            tag = "HR",
            color = 5,
            logo = "/img/mkcv1_images/team_logos/yC17RqXyH5j921D7KQ6t7CWj6ISysnzV9tzgJWyf.png"
        )
    ) {

    }
}