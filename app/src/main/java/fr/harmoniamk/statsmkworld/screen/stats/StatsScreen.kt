package fr.harmoniamk.statsmkworld.screen.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun StatsScreen() {
    BaseScreen(title = "Statistiques") {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            MKText(text = "La consultation de statistiques n'est pas encore disponible. Reviens plus tard !")
        }
    }
}