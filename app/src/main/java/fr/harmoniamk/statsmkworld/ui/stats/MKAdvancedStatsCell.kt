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
 * Section « Indicateurs avancés » regroupant :
 * - Vague 1 : contribution joueur (vue joueur), régularité (écart-type + amplitude) ;
 * - Vague 2 : marges moyennes victoire/défaite ;
 * - Vague 3 : perf 1ʳᵉ/2ᵉ moitié (vue joueur), invaincu depuis, points perdus en pénalités.
 */
@Composable
fun MKAdvancedStatsCell(stats: Stats?, type: StatsType?) {
    stats?.takeIf { it.warStats.warsPlayed > 0 }?.let { s ->
        val isPlayer = (type as? StatsType.PlayerStats)?.userId != null
        MKExpandableSection(title = stringResource(R.string.advanced_stats_section)) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                // Contribution joueur — vue joueur uniquement (n'a de sens que là).
                if (isPlayer) {
                    s.playerContribution?.let {
                        MKStatRow(
                            stringResource(R.string.player_contribution),
                            "$it%",
                            info = stringResource(R.string.info_player_contribution)
                        )
                    }
                }
                s.scoreStdDev?.let {
                    MKStatRow(
                        stringResource(R.string.score_std_dev),
                        "±$it",
                        info = stringResource(R.string.info_score_std_dev)
                    )
                }
                if (s.scoreMin != null && s.scoreMax != null) {
                    MKStatRow(
                        stringResource(R.string.score_amplitude),
                        "${s.scoreMin} – ${s.scoreMax}",
                        info = stringResource(R.string.info_score_amplitude)
                    )
                }
                s.averageWinMargin?.let {
                    MKStatRow(
                        stringResource(R.string.avg_win_margin),
                        "+$it",
                        info = stringResource(R.string.info_avg_win_margin)
                    )
                }
                s.averageLossMargin?.let {
                    MKStatRow(
                        stringResource(R.string.avg_loss_margin),
                        "-$it",
                        info = stringResource(R.string.info_avg_loss_margin)
                    )
                }
                // Perf par moitié de war — vue joueur (position moyenne du joueur).
                if (isPlayer) {
                    s.firstHalfAvgPosition?.let {
                        MKStatRow(
                            stringResource(R.string.first_half_position),
                            it.toString(),
                            info = stringResource(R.string.info_first_half_position)
                        )
                    }
                    s.secondHalfAvgPosition?.let {
                        MKStatRow(
                            stringResource(R.string.second_half_position),
                            it.toString(),
                            info = stringResource(R.string.info_second_half_position)
                        )
                    }
                }
                if (s.unbeatenStreak > 0) {
                    MKStatRow(
                        stringResource(R.string.unbeaten_streak),
                        stringResource(R.string.unbeaten_value, s.unbeatenStreak),
                        info = stringResource(R.string.info_unbeaten_streak)
                    )
                }
                if (s.penaltyPointsLost > 0) {
                    MKStatRow(
                        stringResource(R.string.penalty_points_lost),
                        s.penaltyPointsLost.toString(),
                        info = stringResource(R.string.info_penalty_points_lost)
                    )
                }
            }
        }
    }
}
