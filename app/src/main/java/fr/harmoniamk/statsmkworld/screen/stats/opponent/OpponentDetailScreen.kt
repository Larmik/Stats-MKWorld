package fr.harmoniamk.statsmkworld.screen.stats.opponent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel
import fr.harmoniamk.statsmkworld.ui.stats.BalanceCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.StatTile
import fr.harmoniamk.statsmkworld.ui.stats.StatTiles
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Fiche détail ADVERSAIRE (`opp` du prototype, pôle Classements #27). Sections, dans
 * l'ordre de la maquette :
 * 1. En-tête (logo/tag, nb de confrontations, dernière rencontre) ;
 * 2. Bilan face à eux (winrate + V/N/D + barre) ;
 * 3. 5 dernières face à eux (pastilles V/N/D) ;
 * 4. Séries & scores (série en cours, record, score moyen pour/contre) ;
 * 5. Meilleur circuit contre eux ;
 * 6. Historique des wars → WarDetailsScreen.
 *
 * Rendu pixel-perfect maquette (rules 13/15), cartes partagées ([StatCard]…), données
 * réelles uniquement.
 */
@Composable
fun OpponentDetailScreen(
    viewModel: OpponentDetailViewModel,
    onBack: () -> Unit,
    onWarDetailsClick: (WarDetails) -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(title = stringResource(R.string.opponent_detail_title)) {
        when {
            state.loading -> CircularProgressIndicator()
            else -> {
                val team = state.team
                val stats = state.stats
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. En-tête.
                    item {
                        StatHeaderCard(
                            name = team?.name.orEmpty(),
                            subtitle = stringResource(
                                R.string.opponent_detail_header,
                                stats?.warStats?.warsPlayed ?: 0,
                                state.lastMeeting ?: "-"
                            ),
                            color = Colors.red,
                            logo = team?.logo?.let { "https://mkcentral.com$it" },
                            fallbackText = team?.tag
                        )
                    }
                    // 2. Bilan face à eux.
                    stats?.let { s ->
                        val played = s.warStats.warsPlayed.takeIf { it > 0 } ?: 1
                        item {
                            BalanceCard(
                                title = stringResource(R.string.opponent_detail_balance),
                                winrate = (s.warStats.warsWon * 100) / played,
                                won = s.warStats.warsWon,
                                tied = s.warStats.warsTied,
                                loss = s.warStats.warsLoss,
                                subtitle = stringResource(R.string.opponent_detail_winrate_sub, s.warStats.warsPlayed)
                            )
                        }
                    }
                    // 3. 5 dernières face à eux.
                    if (state.recentOutcomes.isNotEmpty()) item {
                        StatCard(title = stringResource(R.string.opponent_detail_recent)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Plus récente à droite (ordre chronologique) — cohérent avec la maquette.
                                state.recentOutcomes.forEach { OutcomeChipSmall(it) }
                            }
                        }
                    }
                    // 4. Séries & scores.
                    stats?.let { s ->
                        item {
                            StatCard(title = stringResource(R.string.opponent_detail_streaks_scores)) {
                                StatTiles(
                                    tiles = listOf(
                                        StatTile(
                                            label = stringResource(R.string.opponent_detail_current_streak),
                                            value = streakLabel(s.currentStreak),
                                            accent = streakColor(s.currentStreak)
                                        ),
                                        StatTile(
                                            // Record = plus longue série de victoires face à eux.
                                            label = stringResource(R.string.opponent_detail_record),
                                            value = "${s.bestWinStreak} ${stringResource(R.string.v)}",
                                            accent = Colors.green
                                        ),
                                        StatTile(
                                            label = stringResource(R.string.opponent_detail_avg_for),
                                            value = state.averageScoreFor.toString()
                                        ),
                                        StatTile(
                                            label = stringResource(R.string.opponent_detail_avg_against),
                                            value = state.averageScoreAgainst.toString()
                                        )
                                    )
                                )
                            }
                        }
                    }
                    // 5. Meilleur circuit contre eux.
                    state.bestTrack?.let { track ->
                        track.map?.firstOrNull()?.let { map ->
                            item {
                                StatCard(title = stringResource(R.string.opponent_detail_best_track)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Image(
                                            painter = painterResource(map.picture),
                                            contentDescription = null,
                                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                        Column(Modifier.weight(1f)) {
                                            MKText(text = stringResource(map.label), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start)
                                            MKText(
                                                text = stringResource(R.string.opponent_detail_best_track_sub, track.totalPlayed),
                                                textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start,
                                                modifier = Modifier.padding(top = 3.dp)
                                            )
                                        }
                                        MKText(text = "${track.winRate ?: 0}%", font = Fonts.Urbanist, textColor = Colors.green, fontSize = 18)
                                    }
                                }
                            }
                        }
                    }
                    // 6. Historique des wars → WarDetailsScreen.
                    if (state.history.isNotEmpty()) {
                        item { SectionLabel(stringResource(R.string.opponent_detail_history)) }
                        items(state.history, key = { it.war.id }) { war ->
                            WarCell(
                                viewModel = hiltViewModel(
                                    key = war.war.id.toString(),
                                    creationCallback = { factory: WarCellViewModel.Factory -> factory.create(war) }
                                ),
                                onClick = onWarDetailsClick
                            )
                        }
                        item { Spacer(Modifier.height(90.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    MKText(
        text = text.uppercase(),
        font = Fonts.NunitoBD,
        textColor = Colors.white66,
        fontSize = 11,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
    )
}

/** Pastille V/N/D compacte pour la bande « 5 dernières » (vert/blanc/rouge). */
@Composable
private fun OutcomeChipSmall(outcome: Int) {
    val (label, color) = when {
        outcome > 0 -> stringResource(R.string.v) to Colors.green
        outcome < 0 -> stringResource(R.string.d) to Colors.red
        else -> stringResource(R.string.n) to Colors.white
    }
    Box(
        Modifier.size(30.dp).background(color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = label, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 13)
    }
}

@Composable
private fun streakLabel(streak: Int): String = when {
    streak > 0 -> "$streak ${stringResource(R.string.v)}"
    streak < 0 -> "${-streak} ${stringResource(R.string.d)}"
    else -> stringResource(R.string.no_streak)
}

private fun streakColor(streak: Int) = when {
    streak > 0 -> Colors.green
    streak < 0 -> Colors.red
    else -> Colors.white
}
