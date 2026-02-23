package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun MapCell(
    modifier: Modifier = Modifier,
    map: List<Maps>? = null,
    track: WarTrackDetails? = null,
    backgroundColor: Color = Colors.blackAlphaed,
    textColor: Color = Colors.white,
    borderColor: Color = Colors.white,
    onClick: (List<Maps>) -> Unit,
    trackRanking: RankingItem.TrackRanking? = null,
    userId: String? = null,
    is24p: Boolean = false,
    onTrackDetails: (WarTrackDetails) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val maps = Maps.entries
    val mapToDisplay =
        map ?: trackRanking?.stats?.map ?: track?.index?.map { Maps.entries[it.toInt()] }.orEmpty()
    val isIntermission = mapToDisplay.size > 1 || track?.index.orEmpty().size > 1

    Column(
        modifier
            .background(backgroundColor, RoundedCornerShape(5.dp))
            .border(2.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable {
                keyboardController?.hide()
                track?.let(onTrackDetails) ?: run { onClick(mapToDisplay) }
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            when (isIntermission) {
                true -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        maps.getOrNull(mapToDisplay.firstOrNull()?.ordinal ?: -1)?.let { mapFrom ->
                            Image(
                                painter = painterResource(mapFrom.picture),
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(35.dp),
                                contentDescription = null
                            )
                        }
                        maps.getOrNull(mapToDisplay.lastOrNull()?.ordinal ?: -1)?.let { mapTo ->
                            MKText(
                                text = " >>> ",
                                fontSize = 10,
                                textColor = textColor,
                                font = Fonts.NunitoBdIt
                            )
                            Image(
                                painter = painterResource(mapTo.picture),
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(35.dp),
                                contentDescription = null
                            )
                        }
                    }

                    maps.getOrNull(mapToDisplay.lastOrNull()?.ordinal ?: -1)?.let { mapTo ->
                        MKText(
                            text = stringResource(mapTo.label),
                            font = Fonts.NunitoBD,
                            textColor = textColor,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                        MKText(
                            text = mapTo.name,
                            fontSize = 10,
                            textColor = textColor,
                            font = Fonts.NunitoIT,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )

                    }
                    maps.getOrNull(mapToDisplay.firstOrNull()?.ordinal ?: -1)?.let { mapFrom ->
                        MKText(
                            text = "depuis",
                            fontSize = 10,
                            textColor = textColor,
                            font = Fonts.NunitoIT
                        )
                        MKText(
                            text = stringResource(mapFrom.label),
                            fontSize = 12,
                            font = Fonts.NunitoBdIt,
                            textColor = textColor
                        )
                    }

                }

                else -> mapToDisplay.forEach {
                    Image(
                        painter = painterResource(it.cup),
                        modifier = Modifier.size(25.dp),
                        contentDescription = null
                    )
                    Image(
                        painter = painterResource(it.picture),
                        modifier = Modifier
                            .width(90.dp)
                            .height(50.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.height(10.dp))
                    MKText(
                        text = stringResource(it.label),
                        font = Fonts.NunitoBD,
                        textColor = textColor,
                        maxLines = 1
                    )
                    MKText(
                        text = it.name,
                        fontSize = 10,
                        textColor = textColor,
                        font = Fonts.NunitoIT
                    )
                }
            }
        }

        when (val total =
            track?.track?.shocks?.takeIf { it.isNotEmpty() }.orEmpty().sumOf { it.count }) {
            0 -> if (trackRanking == null) Spacer(Modifier.size(20.dp))
            else -> Row {
                (0 until total).forEach { i ->
                    Image(
                        painter = painterResource(R.drawable.shock),
                        modifier = Modifier.size(20.dp),
                        contentDescription = null
                    )
                }
            }
        }

        track?.let {
            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(
                    text = when (is24p) {
                        true -> it.teamScore.toString()
                        else -> it.displayedResult
                    },
                    fontSize = 24,
                    font = Fonts.NunitoBD,
                    textColor = textColor
                )
                if (!is24p)
                    MKText(
                        text = it.displayedDiff,
                        fontSize = 18,
                        font = Fonts.NunitoBD,
                        textColor = textColor
                    )
            }
        }
        trackRanking?.let {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(
                    text = String.format(
                        stringResource(R.string.times_played),
                        it.stats.totalPlayed.toString()
                    ), fontSize = 12,
                    textColor = Colors.white
                )
                if (!is24p)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MKText(
                            text = stringResource(R.string.winrate), fontSize = 12,
                            textColor = Colors.white
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        MKText(
                            text = "${trackRanking.stats.winRate}%",
                            font = Fonts.NunitoBD,
                            fontSize = 12,
                            textColor = Colors.white
                        )
                    }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MKText(
                        text = when (userId) {
                            null -> stringResource(R.string.average_score)
                            else -> stringResource(R.string.average_position)
                        },
                        fontSize = 12,
                        textColor = Colors.white
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    MKText(
                        text = when {
                            userId == null -> if (is24p) it.stats.teamScore.toString() else it.stats.teamScore?.trackScoreToDiff()
                                .toString()

                            it.stats.playerScore?.pointsToPosition(is24p)
                                ?.singleOrNull() != null -> it.stats.playerScore.pointsToPosition(
                                is24p
                            ).singleOrNull().toString()

                            else -> "${
                                it.stats.playerScore?.pointsToPosition(is24p)?.firstOrNull()
                            } - ${it.stats.playerScore?.pointsToPosition(is24p)?.lastOrNull()}"
                        },
                        font = Fonts.NunitoBD,
                        textColor = Colors.white
                    )
                }

            }
        }
    }
}

@Preview
@Composable
fun MapCellPreview() {
    MapCell(map = listOf(Maps.MBC), onClick = {}) {

    }
}

@Preview
@Composable
fun IntermissionCellPreview() {
    MapCell(map = listOf(Maps.MBC, Maps.rSGB), onClick = {}) {

    }
}