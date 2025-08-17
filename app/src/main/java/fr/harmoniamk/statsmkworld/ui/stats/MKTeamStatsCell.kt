package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell

@Composable
fun MKTeamStatsView(stats: Stats?) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        ) {
            stats?.mostPlayedTeam?.let {
                    MKText(text = stringResource(R.string.adversaire_le_plus_joue),  modifier = Modifier.padding(bottom = 5.dp))
                    TeamCell(
                        modifier = Modifier.fillMaxWidth(0.5f),
                        team = it.team,
                        teamStatsLabel = it.totalPlayed?.let {
                            stringResource(
                                R.string.opponent_times_played,
                                it
                            ) },
                        onClick = { })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
            stats?.mostDefeatedTeam?.let {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = stringResource(R.string.le_plus_vaincu),  modifier = Modifier.padding(bottom = 5.dp))
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)) {
                            TeamCell(
                                Modifier.fillMaxWidth(),
                                team = it.team,
                                teamStatsLabel = it.totalPlayed?.let {
                                    stringResource(
                                        R.string.victoires,
                                        it
                                    ) },
                                onClick = { })
                    }
                }
            }
            stats?.lessDefeatedTeam?.let {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = stringResource(R.string.le_moins_vaincu),  modifier = Modifier.padding(bottom = 5.dp))
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)) {
                        TeamCell(
                            Modifier.fillMaxWidth(),
                            team = it.team,
                            teamStatsLabel = it.totalPlayed?.let {
                                stringResource(
                                    R.string.d_faites,
                                    it
                                ) },
                            onClick = { })
                    }
                }
            }

        }

}