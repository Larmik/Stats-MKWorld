package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.Locale

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun MKWarDetailsStatsView(stats: Stats?, mapStats: MapStats?, type: StatsType?) {

    val userId = (type as? StatsType.PlayerStats)?.userId ?: (type as? StatsType.OpponentStats)?.userId ?: (type as? StatsType.MapStats)?.userId

    Column(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
            .background(
                color = Colors.blackAlphaed,
                shape = RoundedCornerShape(5.dp)
            )
    ) {

        if (type !is StatsType.MapStats)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MKText(text = stringResource(R.string.average_score),
                        textColor = Colors.white)
                    MKText(
                        text = when (userId) {
                            null -> stats?.averagePointsLabel.toString()
                            else -> stats?.averagePoints.toString()
                        } ,
                        font = Fonts.Urbanist, fontSize = 20, textColor = Colors.white)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = stringResource(R.string.maps_gagn_es),
                        textColor = Colors.white)
                    MKText(text = stats?.mapsWon.toString(), fontSize = 16, font = Fonts.NunitoBD,
                        textColor = Colors.white)
                }

            }


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(
                    text =  when (userId) {
                        null -> stringResource(R.string.moyenne_map)
                        else -> stringResource(R.string.average_position)
                    },
                    textColor = Colors.white
                )
                MKText(
                    text =  when (userId) {
                        null -> (stats?.averageMapPointsLabel ?: mapStats?.teamScore?.trackScoreToDiff()).toString()
                        else -> (stats?.averagePlayerPosition ?: mapStats?.playerPosition).toString()
                    },

                    font = Fonts.Urbanist, fontSize = 20, textColor = Colors.white
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val shockLabel = when (type) {
                    is StatsType.MapStats -> "Shocks"
                    else -> "Shocks/War"
                }
                val shockCount = when (stats) {
                    null ->  mapStats?.shockCount.toString()
                    else -> String.format(Locale.getDefault(), "%.2f", (stats.shockCount.toFloat() / stats.warStats.warsPlayed))
                }
                MKText(text = shockLabel,
                    textColor = Colors.white)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.shock),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    MKText(
                        text = shockCount,
                        fontSize = 16,
                        font = Fonts.Urbanist,
                        textColor = Colors.white
                    )
                }
            }
        }
    }
}