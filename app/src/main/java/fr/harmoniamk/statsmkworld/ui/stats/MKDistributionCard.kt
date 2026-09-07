package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Histogramme de répartition des positions (P1→P12 en 12p), mutualisé (#27) entre Statistiques
 * et fiches Adversaire/Circuit. Barres ancrées sur une ligne de base commune, couleur par
 * position ([Int.positionColor]). [distribution] = liste (position → occurrences).
 */
@Composable
fun ColumnScope.DistributionChart(distribution: List<Pair<Int, Int>>) {
    val max = distribution.maxOf { it.second }.takeIf { it > 0 } ?: 1
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        distribution.forEach { (position, count) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = count.toString(), font = Fonts.Urbanist, textColor = Colors.white70, fontSize = 8)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((6 + 100 * (count.toFloat() / max)).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(position.positionColor())
                        )
                    }
                }
                MKText(text = position.toString(), font = Fonts.MKPosition, textColor = Colors.white55, fontSize = 8, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Pied de la distribution : Top6 / Bot6 (compte + %), sur la distribution fournie. */
@Composable
fun ColumnScope.DistributionFooter(distribution: List<Pair<Int, Int>>) {
    val total = distribution.sumOf { it.second }.takeIf { it > 0 } ?: 1
    val top6 = distribution.filter { it.first in 1..6 }.sumOf { it.second }
    val bot6 = distribution.filter { it.first in 7..12 }.sumOf { it.second }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        DistributionFooterStat(top6, "Top 6", (top6 * 100) / total, Colors.green)
        DistributionFooterStat(bot6, "Bot 6", (bot6 * 100) / total, Colors.red)
    }
}

@Composable
private fun DistributionFooterStat(count: Int, label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MKText(text = count.toString(), font = Fonts.Urbanist, textColor = color, fontSize = 13)
        MKText(text = "$label · $percent %", textColor = Colors.white70, fontSize = 11)
    }
}
