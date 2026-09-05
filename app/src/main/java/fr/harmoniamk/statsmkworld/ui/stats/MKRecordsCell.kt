package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType

/**
 * Lot A — section « Records & séries » : série en cours, records de séries
 * de victoires/défaites, comptes Top6/Bot6 (affichés si > 0). Repliée par défaut
 * pour garder l'écran scannable.
 */
@Composable
fun MKRecordsCell(stats: Stats?, type: StatsType?) {
    stats?.let {
        MKExpandableSection(title = stringResource(R.string.records_series)) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                MKStatRow(
                    label = stringResource(R.string.current_streak),
                    value = it.currentStreak.streakLabel(),
                    info = stringResource(R.string.info_current_streak)
                )
                MKStatRow(
                    label = stringResource(R.string.best_win_streak),
                    value = stringResource(R.string.win_streak_value, it.bestWinStreak),
                    info = stringResource(R.string.info_best_win_streak)
                )
                MKStatRow(
                    label = stringResource(R.string.worst_loss_streak),
                    value = stringResource(R.string.loss_streak_value, it.worstLossStreak),
                    info = stringResource(R.string.info_worst_loss_streak)
                )
                // Comptes bruts Top6/Bot6 (déjà calculés dans Stats) : affichés
                // seulement s'ils sont > 0.
                if (it.top6Count > 0)
                    MKStatRow(
                        label = stringResource(R.string.top6_count),
                        value = it.top6Count.toString(),
                        info = stringResource(R.string.info_top6_count)
                    )
                if (it.bot6Count > 0)
                    MKStatRow(
                        label = stringResource(R.string.bot6_count),
                        value = it.bot6Count.toString(),
                        info = stringResource(R.string.info_bot6_count)
                    )
            }
        }
    }
}

@Composable
private fun Int.streakLabel(): String = when {
    this > 0 -> stringResource(R.string.win_streak_value, this)
    this < 0 -> stringResource(R.string.loss_streak_value, -this)
    else -> stringResource(R.string.no_streak)
}
