package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun MKPlayerScoreCell(stats: Stats?, type: StatsType?) {
    val userId = (type as? StatsType.PlayerStats)?.userId

    Column(Modifier.padding(bottom = 20.dp)) {
        MKText(
            text = "Scores",
            fontSize = 16,
            font = Fonts.NunitoBD,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row {
            Column(
                Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(text = when (userId) {
                    null -> "Plus large victoire"
                    else -> "Pire score"
                }, fontSize = 12)
                MKText(
                    text = when (userId) {
                        null -> stats?.warStats?.highestVictory?.displayedScore.toString()
                        else -> stats?.lowestPlayerScore?.first?.toString() ?: stats?.lowestScore?.score.toString()
                    },
                    fontSize = 20,
                    font = Fonts.Urbanist
                )
                MKText(text = stats?.lowestScore?.opponentLabel.toString())
                //MKText(text = stats.lowestScore?.war?.war?.createdDate.toString(), fontSize = 12)
            }
            Column(
                Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(text = when (userId) {
                    null -> "Plus lourde défaite"
                    else -> "Meilleur score"
                }, fontSize = 12)
                MKText(
                    text = when (userId) {
                        null -> stats?.warStats?.loudestDefeat?.displayedScore.toString()
                        else -> stats?.highestPlayerScore?.first?.toString() ?: stats?.highestScore?.score.toString()
                    },
                    fontSize = 20,
                    font = Fonts.Urbanist
                )
                MKText(text = stats?.highestScore?.opponentLabel.toString())
               // MKText(text = stats.highestScore?.war?.war?.createdDate.toString(), fontSize = 12)
            }
        }
    }
}