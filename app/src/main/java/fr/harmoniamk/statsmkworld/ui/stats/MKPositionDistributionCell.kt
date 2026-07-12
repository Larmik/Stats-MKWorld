package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Vague 2 — mini-histogramme de la distribution des positions du joueur
 * (P1→P12, 12p). Vue joueur uniquement.
 */
@Composable
fun MKPositionDistributionCell(stats: Stats?, type: StatsType?) {
    stats?.positionDistribution
        ?.takeIf { it.any { entry -> entry.second > 0 } }
        ?.let { distribution ->
            val max = distribution.maxOf { it.second }.takeIf { it > 0 } ?: 1
            MKExpandableSection(title = stringResource(R.string.position_distribution_section)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    distribution.forEach { (position, count) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MKText(
                                text = position.toString(),
                                font = Fonts.MKPosition,
                                textColor = position.positionColor(),
                                fontSize = 16,
                                modifier = Modifier.width(30.dp)
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(count.toFloat() / max)
                                    .height(14.dp)
                                    .background(position.positionColor(), RoundedCornerShape(3.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                            MKText(text = count.toString(), textColor = Colors.white, fontSize = 12)
                        }
                    }
                }
            }
        }
}
