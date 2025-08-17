package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun MKWinTieLossCell(stats: Stats?, mapStats: MapStats?) {
    val warStats = stats?.warStats
    val played = warStats?.warsPlayed ?: mapStats?.trackPlayed
    val win = warStats?.warsWon ?: mapStats?.trackWon ?: 0
    val tie = warStats?.warsTied ?: mapStats?.trackTie ?: 0
    val loss = warStats?.warsLoss ?: mapStats?.trackLoss ?: 0
    val winRate = (win * 100) / (played?.takeIf { it > 0 } ?: 1)

    Column(
        modifier = Modifier
            .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
            .background(
                color = Colors.blackAlphaed,
                shape = RoundedCornerShape(5.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.padding(5.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = stringResource(R.string.v), font = Fonts.NunitoBD, modifier = Modifier.padding(vertical = 5.dp), textColor = Colors.white)
                MKText(text = win.toString(), font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 5.dp), fontSize = 20, textColor = Colors.white)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = stringResource(R.string.n), font =  Fonts.NunitoBD, modifier = Modifier.padding(vertical = 5.dp), textColor = Colors.white)
                MKText(text = tie.toString(), font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 5.dp), fontSize = 20, textColor = Colors.white)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = stringResource(R.string.d), font = Fonts.NunitoBD, modifier = Modifier.padding(vertical = 5.dp), textColor = Colors.white)
                MKText(text = loss.toString(), font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 5.dp), fontSize = 20, textColor = Colors.white)
            }
        }
        MKLineChart(stats = stats, mapStats = mapStats)
        MKText(text = stringResource(R.string.winrate_placeholder, "$winRate%"), font = Fonts.NunitoBD, modifier = Modifier.padding(vertical = 5.dp), textColor = Colors.white)
    }
}
