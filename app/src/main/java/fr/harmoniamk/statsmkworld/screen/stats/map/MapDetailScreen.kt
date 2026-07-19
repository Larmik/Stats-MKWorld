package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.BalanceCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.StatTile
import fr.harmoniamk.statsmkworld.ui.stats.StatTiles
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import fr.harmoniamk.statsmkworld.ui.stats.mapStatsDetailSections
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Fiche détail CIRCUIT (`map` du prototype, pôle Classements #27). Sections, dans l'ordre
 * de la maquette :
 * 1. En-tête (nom, coupe, nb de fois joué) ;
 * 2. Performance (winrate de manche + V/N/D + barre) ;
 * 3. Scores moyens (score équipe / position moyenne) ;
 * 4. Top 6 / Bot 6 (places de l'équipe sur ce circuit) ;
 * 5. Meilleur pilote ici (winrate perso).
 *
 * Rendu pixel-perfect maquette (rules 13/15), cartes partagées, données réelles.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun MapDetailScreen(
    viewModel: MapDetailViewModel,
    onBack: () -> Unit
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
                    // 3. Scores moyens.
                    item {
                        StatCard(title = stringResource(R.string.map_detail_avg_scores)) {
                            StatTiles(
                                tiles = listOf(
                                    StatTile(
                                        label = stringResource(R.string.map_detail_team_score),
                                        value = mapStats.teamScore.toString()
                                    ),
                                    StatTile(
                                        label = stringResource(R.string.map_detail_avg_position),
                                        value = mapStats.averagePlayerPosLabel
                                    )
                                )
                            )
                        }
                    }
                    // 4. Top 6 / Bot 6 (places de l'équipe sur ce circuit).
                    item {
                        val top6 = mapStats.topsTable.firstOrNull { it.first == "Top 6" }?.second ?: 0
                        val bot6 = mapStats.bottomsTable.firstOrNull { it.first == "Bot 6" }?.second ?: 0
                        StatCard(title = stringResource(R.string.map_detail_top_bottom)) {
                            StatTiles(
                                tiles = listOf(
                                    StatTile(
                                        label = stringResource(R.string.map_detail_top6),
                                        value = "${top6}×",
                                        accent = Colors.green,
                                        borderColor = Colors.green
                                    ),
                                    StatTile(
                                        label = stringResource(R.string.map_detail_bot6),
                                        value = "${bot6}×",
                                        accent = Colors.red,
                                        borderColor = Colors.red
                                    )
                                )
                            )
                        }
                    }
                    // 5. Meilleur pilote ici.
                    state.bestPilot?.let { pilot ->
                        item {
                            StatCard(title = stringResource(R.string.map_detail_best_pilot)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape).background(Colors.blue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MKText(text = initialsOf(pilot.player.name), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 13)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        MKText(text = pilot.player.name, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start)
                                        MKText(
                                            text = stringResource(R.string.map_detail_pilot_score, pilot.averageScore),
                                            textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start,
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                    MKText(text = "${pilot.winrate}%", font = Fonts.Urbanist, textColor = Colors.green, fontSize = 18)
                                }
                            }
                        }
                    }
                    // 6. Sections détaillées mutualisées (mêmes calculs que StatsFullScreen,
                    //    scopées au circuit) : répartition des positions, Top/Bot 2→6, shocks.
                    mapStatsDetailSections(mapStats)
                }
            }
        }
    }
}

/** Petite ligne « coupe » sous l'en-tête : icône de la coupe du circuit (pas de nom de coupe en ressource). */
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
