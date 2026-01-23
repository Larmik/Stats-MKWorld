package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun MKPlayerScoreCell(stats: Stats?, type: StatsType?) {
    val userId = (type as? StatsType.PlayerStats)?.userId
    Row(Modifier.padding(bottom = 20.dp)) {
        Column(
            Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MKText(text = when (userId) {
                null -> stringResource(R.string.plus_large_victoire)
                else -> stringResource(R.string.pire_score)
            })
            MKText(
                text = when (userId) {
                    null -> stats?.warStats?.highestVictory?.displayedScore ?: stringResource(R.string.aucune)
                    else -> stats?.lowestPlayerScore?.first?.toString() ?: stats?.lowestScore?.score.toString()
                },
                fontSize = 20,
                font = Fonts.Urbanist
            )
            MKText(text = when (userId) {
                null -> (stats?.warStats?.highestVictory?.war)?.let { WarEntity(it).createdDate }.orEmpty()
                else -> stats?.lowestScore?.war?.date.orEmpty()
            }, fontSize = 12)
        }
        Column(
            Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MKText(text = when (userId) {
                null -> stringResource(R.string.plus_lourde_d_faite)
                else -> stringResource(R.string.meilleur_score)
            })
            MKText(
                text = when (userId) {
                    null -> stats?.warStats?.loudestDefeat?.displayedScore ?: stringResource(R.string.aucune)
                    else -> stats?.highestPlayerScore?.first?.toString() ?: stats?.highestScore?.score.toString()
                },
                fontSize = 20,
                font = Fonts.Urbanist
            )
            MKText(text = when (userId) {
                null -> (stats?.warStats?.loudestDefeat?.war)?.let { WarEntity(it).createdDate }.orEmpty()
                else -> stats?.highestScore?.war?.date.orEmpty()
            }, fontSize = 12)
        }
    }
}