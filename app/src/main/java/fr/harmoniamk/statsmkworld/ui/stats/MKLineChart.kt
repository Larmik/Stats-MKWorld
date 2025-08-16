package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.Colors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun MKLineChart(stats: Stats?, mapStats: MapStats?) {
    val win = when {
        stats != null -> stats.warStats.warsWon.toFloat() / stats.warStats.warsPlayed.toFloat()
        mapStats != null -> mapStats.trackWon.toFloat()  / mapStats.trackPlayed.toFloat()
        else -> null
    }
    val tie = when {
        stats != null -> stats.warStats.warsTied.toFloat() / stats.warStats.warsPlayed.toFloat()
        mapStats != null -> mapStats.trackTie.toFloat()  / mapStats.trackPlayed.toFloat()
        else -> null
    }
    val loss = when {
        stats != null -> stats.warStats.warsLoss.toFloat() / stats.warStats.warsPlayed.toFloat()
        mapStats != null -> mapStats.trackLoss.toFloat()  / mapStats.trackPlayed.toFloat()
        else -> null
    }

    Row(Modifier.fillMaxWidth().height(35.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(10.dp).height(35.dp))
        win?.takeIf { it > 0f }?.let {
            val endCorner = when (it) {
                1f -> 20.dp
                else -> 0.dp
            }
            Spacer(Modifier.weight(it*100).height(25.dp).background(Colors.green, shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = endCorner, bottomEnd = endCorner)))
        }
        tie?.takeIf { it > 0f }?.let {
            Spacer(Modifier.weight(it*100).height(25.dp).background(Colors.white))
        }
        loss?.takeIf { it > 0f }?.let {
            val startCorner = when (it) {
                1f -> 20.dp
                else -> 0.dp
            }
            Spacer(Modifier.weight(it*100).height(25.dp).background(Colors.red, shape = RoundedCornerShape(topStart = startCorner, bottomStart = startCorner, topEnd = 20.dp, bottomEnd = 20.dp)))
        }
        Spacer(Modifier.width(10.dp).height(35.dp))
    }
}
