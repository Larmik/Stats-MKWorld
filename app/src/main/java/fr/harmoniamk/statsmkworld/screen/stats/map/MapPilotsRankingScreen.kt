package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius
import fr.harmoniamk.statsmkworld.ui.stats.podiumRows
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Classement COMPLET des pilotes sur un circuit (« Voir le classement en entier » de la
 * fiche circuit #27), du meilleur au pire score perso moyen. Réutilise le même
 * [MapDetailViewModel] (même clé de nav → mêmes données) et la grille `podiumRows`
 * mutualisée (rule 16), texte des cellules en NOIR sur le fond clair du BaseScreen.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun MapPilotsRankingScreen(
    viewModel: MapDetailViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(title = stringResource(R.string.map_detail_pilots), onBack = onBack, modifier = Modifier.padding(bottom = 90.dp)) {
        when {
            state.loading -> CircularProgressIndicator()
            state.pilots.isEmpty() -> MKText(text = stringResource(R.string.stats_no_data), textColor = Colors.white66, fontSize = 13)
            else -> LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(StatCardRadius)
                    .background(Colors.blackAlphaed, StatCardRadius)
                    .border(1.dp, Colors.whiteBorder, StatCardRadius)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cellules en BLANC dans un cadre transparent-noir (#50 pt.7) —
                // harmonisation avec les autres écrans.
                podiumRows(
                    items = state.pilots.map { pilot -> pilot.toPodiumEntry() to pilot },
                    contentColor = Colors.white
                )
            }
        }
    }
}
