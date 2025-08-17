package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun MKWarStatsView(stats: Stats?, mapStats: MapStats?) {
    val totalPlayed = stats?.warStats?.warsPlayed ?: mapStats?.trackPlayed
    val totalPlayedLabel = when  {
        stats != null -> stringResource(R.string.wars_played)
        mapStats != null -> stringResource(R.string.maps_played)
        else -> ""
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 20.dp)) {
            Column(
                Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = totalPlayed.toString(), font = Fonts.Urbanist, fontSize = 26)
                MKText(text = totalPlayedLabel, font = Fonts.Urbanist, fontSize = 20)
                Spacer(modifier = Modifier.height(15.dp))
                MKWinTieLossCell(stats = stats, mapStats = mapStats)

            }
        }
    }
}