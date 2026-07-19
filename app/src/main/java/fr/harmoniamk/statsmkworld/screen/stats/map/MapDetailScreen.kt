package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.BalanceCard
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.PodiumSectionCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.StatTile
import fr.harmoniamk.statsmkworld.ui.stats.StatTiles
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import fr.harmoniamk.statsmkworld.ui.stats.mapStatsDetailSections
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Fiche détail CIRCUIT (`map` du prototype, pôle Classements #27). Sélecteur Indiv/Équipe
 * (rule 11 : bascule un état réactif du VM, l'écran reste monté). Sections :
 * 1. Sélecteur Indiv/Équipe + en-tête (nom, coupe, nb de fois joué) ;
 * 2. Performance (winrate de manche + V/N/D + barre) ;
 * 3. Scores moyens (score équipe/perso · position moyenne · shocks joués) ;
 * 4. Répartition des positions + Top/Bot 2→6 (sections détaillées mutualisées) ;
 * 5. Pilotes sur ce circuit (podium Top3/Flop3 par score moyen + « Voir le classement en entier »).
 *
 * Rendu pixel-perfect maquette (rules 13/15), cartes/podiums partagés (rule 16), données réelles.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun MapDetailScreen(
    viewModel: MapDetailViewModel,
    onBack: () -> Unit,
    onPilotsRanking: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(title = stringResource(R.string.map_detail_title)) {
        when {
            state.loading -> CircularProgressIndicator()
            state.mapStats == null -> MKText(
                text = stringResource(R.string.stats_no_data),
                textColor = Colors.white66,
                fontSize = 13
            )
            else -> {
                val mapStats = state.mapStats!!
                val map = state.maps.firstOrNull()
                // Sélecteur Indiv/Équipe (rule 15 : composant partagé) — sur fond clair (onDark false).
                MKSegmentedSelector(
                    items = listOf(
                        stringResource(R.string.map_detail_scope_indiv),
                        stringResource(R.string.map_detail_scope_team)
                    ),
                    page = if (state.isIndiv) 0 else 1,
                    onClick = { index -> viewModel.onModeChange(index == 0) }
                )
                Spacer(Modifier.height(11.dp))
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. En-tête (pastille illustration + nom + « joué N fois » + icône de coupe).
                    item {
                        StatHeaderCard(
                            name = map?.label?.let { stringResource(it) } ?: "-",
                            subtitle = stringResource(R.string.map_detail_header, mapStats.trackPlayed),
                            color = Colors.purple,
                            pictureRes = map?.picture
                        )
                        map?.let { CupRow(it.cup) }
                    }
                    // 2. Performance (winrate de manche + V/N/D).
                    item {
                        val played = mapStats.trackPlayed.takeIf { it > 0 } ?: 1
                        BalanceCard(
                            title = stringResource(R.string.map_detail_performance),
                            winrate = (mapStats.trackWon * 100) / played,
                            won = mapStats.trackWon,
                            tied = mapStats.trackTie,
                            loss = mapStats.trackLoss,
                            subtitle = stringResource(R.string.map_detail_winrate_sub, mapStats.trackPlayed)
                        )
                    }
                    // 3. Scores moyens (score · position moyenne · shocks joués).
                    item {
                        StatCard(title = stringResource(R.string.map_detail_avg_scores)) {
                            StatTiles(
                                tiles = listOf(
                                    StatTile(
                                        label = stringResource(
                                            if (state.isIndiv) R.string.map_detail_your_score else R.string.map_detail_team_score
                                        ),
                                        value = state.averageScore.toString()
                                    ),
                                    StatTile(
                                        // Position moyenne réelle (joueur en indiv, équipe sinon).
                                        label = stringResource(R.string.map_detail_avg_position),
                                        value = state.averagePositionLabel
                                    ),
                                    StatTile(
                                        label = stringResource(R.string.stats_shocks_played),
                                        value = mapStats.shockCount.toString()
                                    )
                                )
                            )
                        }
                    }
                    // 4. Sections détaillées mutualisées : répartition des positions, Top/Bot 2→6.
                    mapStatsDetailSections(mapStats)
                    // 5. Classement des pilotes sur ce circuit (par score moyen) + voir en entier.
                    if (state.pilots.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.map_detail_pilots),
                            top = state.pilots.take(3).map { it.toPodiumEntry() },
                            flop = state.pilots.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onPilotsRanking
                        )
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

/**
 * Pilote → entrée de podium (initiales, score perso moyen + winrate). Partagé entre la
 * fiche (podium Top3/Flop3) et le classement complet [MapPilotsRankingScreen].
 */
internal fun MapDetailViewModel.PilotRanking.toPodiumEntry(): PodiumEntry =
    PodiumEntry(
        name = player.name,
        initials = initialsOf(player.name),
        stats = listOf(
            R.string.form_score to averageScore.toString(),
            R.string.form_winrate to "$winrate%"
        )
    )

/** Petite ligne « coupe » sous l'en-tête : icône de la coupe (pas de nom de coupe en ressource). */
@Composable
private fun CupRow(cup: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(cup),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        MKText(text = stringResource(R.string.map_detail_cup), textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start)
    }
}
