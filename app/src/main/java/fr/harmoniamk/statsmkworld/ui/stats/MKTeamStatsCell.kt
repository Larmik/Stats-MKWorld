package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun MKTeamStatsView(stats: Stats?) {
    Column(Modifier.padding(bottom = 20.dp)) {
        MKText(text = "Adversaires", fontSize = 16, font = Fonts.NunitoBD, modifier = Modifier.padding(bottom = 10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()) {
            MKText(text = "Le plus joué", fontSize = 12)
            MKText(text = stats?.mostPlayedTeam?.team?.name.toString(), font = Fonts.NunitoBD)
            MKText(text = String.format("Joué %s fois", stats?.mostPlayedTeam?.totalPlayed.toString()), fontSize = 12)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            stats?.mostDefeatedTeam?.team?.let {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = "Le plus vaincu", fontSize = 12)
                    MKText(text = stats?.mostDefeatedTeam?.team?.name.toString(), font = Fonts.NunitoBD)
                    MKText(text = String.format("%s victoires", stats?.mostDefeatedTeam?.totalPlayed.toString()), fontSize = 12)
                }
            }
            stats?.lessDefeatedTeam?.team?.let {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = "Le moins vaincu", fontSize = 12)
                    MKText(text = stats?.lessDefeatedTeam?.team?.name.toString(), font = Fonts.NunitoBD)
                    MKText(text = String.format("%s défaites", stats?.lessDefeatedTeam?.totalPlayed.toString()), fontSize = 12)
                }
            }

        }
    }
}